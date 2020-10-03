///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.websocket;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;
import io.crossbar.autobahn.websocket.messages.BinaryMessage;
import io.crossbar.autobahn.websocket.messages.Close;
import io.crossbar.autobahn.websocket.messages.ConnectionLost;
import io.crossbar.autobahn.websocket.messages.Error;
import io.crossbar.autobahn.websocket.messages.Ping;
import io.crossbar.autobahn.websocket.messages.Pong;
import io.crossbar.autobahn.websocket.messages.ProtocolViolation;
import io.crossbar.autobahn.websocket.messages.RawTextMessage;
import io.crossbar.autobahn.websocket.messages.ServerError;
import io.crossbar.autobahn.websocket.messages.ServerHandshake;
import io.crossbar.autobahn.websocket.messages.TextMessage;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;
import io.crossbar.autobahn.websocket.utils.Utf8Validator;


/**
 * WebSocket reader, the receiving leg of a WebSockets connection.
 * This runs on it's own background thread and posts messages to master
 * thread's message queue for there to be consumed by the application.
 * The only method that needs to be called (from foreground thread) is quit(),
 * which gracefully shuts down the background receiver thread.
 */
class WebSocketReader extends Thread {

    private static final IABLogger LOGGER = ABLogger.getLogger(WebSocketReader.class.getName());

    private final IThreadMessenger mMessenger;
    private final WebSocketOptions mOptions;

    private BufferedInputStream mBufferedStream;
    private Socket mSocket;
    private int mPosition;
    private byte[] mMessageData;
    private ByteArrayOutputStream mMessagePayload;

    private final static int STATE_CLOSED = 0;
    private final static int STATE_CONNECTING = 1;
    private final static int STATE_CLOSING = 2;
    private final static int STATE_OPEN = 3;

    private boolean mStopped = false;
    private int mState;
    private long mLastReadTime;

    private boolean mInsideMessage = false;
    private int mMessageOpcode;

    /// Frame currently being received.
    private FrameHeader mFrameHeader;

    private Utf8Validator mUtf8Validator = new Utf8Validator();


    /**
     * WebSockets frame metadata.
     */
    private static class FrameHeader {
        public int mOpcode;
        public boolean mFin;
        public int mReserved;
        public int mHeaderLen;
        public int mPayloadLen;
        public int mTotalLen;
        public byte[] mMask;
    }


    /**
     * Create new WebSockets background reader.
     *
     * @param messenger The message handler of master (foreground thread).
     * @param socket The socket channel created on foreground thread.
     */
    public WebSocketReader(IThreadMessenger messenger, Socket socket, WebSocketOptions options, String threadName)
            throws IOException {

        super(threadName);

        mMessenger = messenger;
        mOptions = options;
        mSocket = socket;

        mMessageData = new byte[mOptions.getMaxFramePayloadSize() + 14];
        mBufferedStream = new BufferedInputStream(mSocket.getInputStream(), mOptions.getMaxFramePayloadSize() + 14);
        mMessagePayload = new ByteArrayOutputStream(options.getMaxMessagePayloadSize());

        mFrameHeader = null;
        mState = STATE_CONNECTING;

        LOGGER.d("Created");
    }

    double getTimeSinceLastRead() {
        return Math.round((System.currentTimeMillis() - mLastReadTime) / 1000.0);
    }

    /**
     * Graceful shutdown of background reader thread (called from master).
     */
    public void quit() {
        mState = STATE_CLOSED;
        LOGGER.d("Quit");
    }

    /**
     * Process incoming WebSockets data (after handshake).
     */
    private boolean processData() throws Exception {

        // outside frame?
        if (mFrameHeader == null) {

            // need at least 2 bytes from WS frame header to start processing
            if (mPosition >= 2) {

                byte b0 = mMessageData[0];
                boolean fin = (b0 & 0x80) != 0;
                int rsv = (b0 & 0x70) >> 4;
                int opcode = b0 & 0x0f;

                byte b1 = mMessageData[1];
                boolean masked = (b1 & 0x80) != 0;
                int payload_len1 = b1 & 0x7f;

                // now check protocol compliance

                if (rsv != 0) {
                    throw new WebSocketException("RSV != 0 and no extension negotiated");
                }

                if (masked) {
                    // currently, we don't allow this. need to see whats the final spec.
                    throw new WebSocketException("masked server frame");
                }

                if (opcode > 7) {
                    // control frame
                    if (!fin) {
                        throw new WebSocketException("fragmented control frame");
                    }
                    if (payload_len1 > 125) {
                        throw new WebSocketException("control frame with payload length > 125 octets");
                    }
                    if (opcode != 8 && opcode != 9 && opcode != 10) {
                        throw new WebSocketException("control frame using reserved opcode " + opcode);
                    }
                    if (opcode == 8 && payload_len1 == 1) {
                        throw new WebSocketException("received close control frame with payload len 1");
                    }
                } else {
                    // message frame
                    if (opcode != 0 && opcode != 1 && opcode != 2) {
                        throw new WebSocketException("data frame using reserved opcode " + opcode);
                    }
                    if (!mInsideMessage && opcode == 0) {
                        throw new WebSocketException("received continuation data frame outside fragmented message");
                    }
                    if (mInsideMessage && opcode != 0) {
                        throw new WebSocketException("received non-continuation data frame while inside fragmented message");
                    }
                }

                int mask_len = masked ? 4 : 0;
                int header_len;

                if (payload_len1 < 126) {
                    header_len = 2 + mask_len;
                } else if (payload_len1 == 126) {
                    header_len = 2 + 2 + mask_len;
                } else if (payload_len1 == 127) {
                    header_len = 2 + 8 + mask_len;
                } else {
                    // should not arrive here
                    throw new Exception("logic error");
                }

                // continue when complete frame header is available
                if (mPosition >= header_len) {

                    // determine frame payload length
                    int i = 2;
                    long payload_len;
                    if (payload_len1 == 126) {
                        payload_len = ((0xff & mMessageData[i]) << 8) | (0xff & mMessageData[i + 1]);
                        if (payload_len < 126) {
                            throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
                        }
                        i += 2;
                    } else if (payload_len1 == 127) {
                        if ((0x80 & mMessageData[i]) != 0) {
                            throw new WebSocketException("invalid data frame length (> 2^63)");
                        }
                        payload_len = ((long) (0xff & mMessageData[i]) << 56) |
                                ((long) (0xff & mMessageData[i + 1]) << 48) |
                                ((long) (0xff & mMessageData[i + 2]) << 40) |
                                ((long) (0xff & mMessageData[i + 3]) << 32) |
                                ((long) (0xff & mMessageData[i + 4]) << 24) |
                                ((long) (0xff & mMessageData[i + 5]) << 16) |
                                ((long) (0xff & mMessageData[i + 6]) << 8) |
                                ((long) (0xff & mMessageData[i + 7]));
                        if (payload_len < 65536) {
                            throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
                        }
                        i += 8;
                    } else {
                        payload_len = payload_len1;
                    }

                    // immediately bail out on frame too large
                    if (payload_len > mOptions.getMaxFramePayloadSize()) {
                        throw new WebSocketException("frame payload too large");
                    }

                    // save frame header metadata
                    mFrameHeader = new FrameHeader();
                    mFrameHeader.mOpcode = opcode;
                    mFrameHeader.mFin = fin;
                    mFrameHeader.mReserved = rsv;
                    mFrameHeader.mPayloadLen = (int) payload_len;
                    mFrameHeader.mHeaderLen = header_len;
                    mFrameHeader.mTotalLen = mFrameHeader.mHeaderLen + mFrameHeader.mPayloadLen;
                    if (masked) {
                        mFrameHeader.mMask = new byte[4];
                        for (int j = 0; j < 4; ++j) {
                            mFrameHeader.mMask[i] = (byte) (0xff & mMessageData[i + j]);
                        }
                        i += 4;
                    } else {
                        mFrameHeader.mMask = null;
                    }

                    // continue processing when payload empty or completely buffered
                    return mFrameHeader.mPayloadLen == 0 || mPosition >= mFrameHeader.mTotalLen;

                } else {

                    // need more data
                    return false;
                }
            } else {

                // need more data
                return false;
            }

        } else {

            /// \todo refactor this for streaming processing, incl. fail fast on invalid UTF-8 within frame already

            // within frame

            // see if we buffered complete frame
            if (mPosition >= mFrameHeader.mTotalLen) {

                // cut out frame payload
                byte[] framePayload = null;
                if (mFrameHeader.mPayloadLen > 0) {
                    framePayload = new byte[mFrameHeader.mPayloadLen];
                    System.arraycopy(mMessageData, mFrameHeader.mHeaderLen, framePayload, 0, mFrameHeader.mPayloadLen);
                }

                // mMessageData = Arrays.copyOfRange(mMessageData, mFrameHeader.mTotalLen, mMessageData.length + mFrameHeader.mTotalLen);
                // We were previously recreating the message data array for each fragment, that is resource hungry.
                // Imagine having set the message max payload size to 16M, each time we receive a new frame for a
                // message, we were creating a new byte[] of 16M.
                //
                // Now imagine if the sender sends a message of size 4M in fragments of 64, we will be creating a byte[16M]
                // 4 * 2**20 / 64 times which is 65536, that's one hell of a pressure on the GC.
                System.arraycopy(mMessageData, mFrameHeader.mTotalLen, mMessageData, 0, mMessageData.length - mFrameHeader.mTotalLen);
                mPosition -= mFrameHeader.mTotalLen;

                if (mFrameHeader.mOpcode > 7) {
                    // control frame
                    if (mFrameHeader.mOpcode == 8) {

                        int code = 1005; // CLOSE_STATUS_CODE_NULL : no status code received
                        String reason = null;

                        if (mFrameHeader.mPayloadLen >= 2) {

                            // parse and check close code - see http://tools.ietf.org/html/rfc6455#section-7.4
                            code = (framePayload[0] & 0xff) * 256 + (framePayload[1] & 0xff);
                            if (code < 1000
                                    || (code >= 1000 && code <= 2999 &&
                                    code != 1000 && code != 1001 && code != 1002 && code != 1003 && code != 1007 && code != 1008 && code != 1009 && code != 1010 && code != 1011)
                                    || code >= 5000) {

                                throw new WebSocketException("invalid close code " + code);
                            }

                            // parse and check close reason
                            if (mFrameHeader.mPayloadLen > 2) {

                                byte[] ra = new byte[mFrameHeader.mPayloadLen - 2];
                                System.arraycopy(framePayload, 2, ra, 0, mFrameHeader.mPayloadLen - 2);

                                Utf8Validator val = new Utf8Validator();
                                val.validate(ra);
                                if (!val.isValid()) {
                                    throw new WebSocketException("invalid close reasons (not UTF-8)");
                                } else {
                                    reason = new String(ra, "UTF-8");
                                }
                            }
                        }
                        onClose(code, reason);
                        // We have received a close, so lets set the state as early as possible.
                        // It seems that Handler() has a lag to deliver a message, so if the onClose
                        // is sent to master and just after that the other peer closes the socket,
                        // BufferedInputReader.read() will throw an exception which results in
                        // our code sending a ConnectionLost() message to master.
                        mState = STATE_CLOSED;

                    } else if (mFrameHeader.mOpcode == 9) {
                        // dispatch WS ping
                        onPing(framePayload);

                    } else if (mFrameHeader.mOpcode == 10) {
                        // dispatch WS pong
                        onPong(framePayload);

                    } else {

                        // should not arrive here (handled before)
                        throw new Exception("logic error");
                    }

                } else {
                    // message frame

                    if (!mInsideMessage) {
                        // new message started
                        mInsideMessage = true;
                        mMessageOpcode = mFrameHeader.mOpcode;
                        if (mMessageOpcode == 1 && mOptions.getValidateIncomingUtf8()) {
                            mUtf8Validator.reset();
                        }
                    }

                    if (framePayload != null) {

                        // immediately bail out on message too large
                        if (mMessagePayload.size() + framePayload.length > mOptions.getMaxMessagePayloadSize()) {
                            throw new WebSocketException("message payload too large");
                        }

                        // validate incoming UTF-8
                        if (mMessageOpcode == 1 && mOptions.getValidateIncomingUtf8() && !mUtf8Validator.validate(framePayload)) {
                            throw new WebSocketException("invalid UTF-8 in text message payload");
                        }

                        // buffer frame payload for message
                        mMessagePayload.write(framePayload);
                    }

                    // on final frame ..
                    if (mFrameHeader.mFin) {

                        if (mMessageOpcode == 1) {

                            // verify that UTF-8 ends on codepoint
                            if (mOptions.getValidateIncomingUtf8() && !mUtf8Validator.isValid()) {
                                throw new WebSocketException("UTF-8 text message payload ended within Unicode code point");
                            }

                            // deliver text message
                            if (mOptions.getReceiveTextMessagesRaw()) {

                                // dispatch WS text message as raw (but validated) UTF-8
                                onRawTextMessage(mMessagePayload.toByteArray());

                            } else {

                                // dispatch WS text message as Java String (previously already validated)
                                String s = new String(mMessagePayload.toByteArray(), "UTF-8");
                                onTextMessage(s);
                            }

                        } else if (mMessageOpcode == 2) {

                            // dispatch WS binary message
                            onBinaryMessage(mMessagePayload.toByteArray());

                        } else {

                            // should not arrive here (handled before)
                            throw new Exception("logic error");
                        }

                        // ok, message completed - reset all
                        mInsideMessage = false;
                        mMessagePayload.reset();
                    }
                }

                // reset frame
                mFrameHeader = null;

                // reprocess if more data left
                return mPosition > 0;

            } else {

                // need more data
                return false;
            }
        }
    }


    /**
     * WebSockets handshake reply from server received, default notifies master.
     *
     */
    protected void onHandshake(Map<String, String> handshakeParams) {

        mMessenger.notify(new ServerHandshake(handshakeParams));
    }


    /**
     * WebSockets close received, default notifies master.
     */
    protected void onClose(int code, String reason) {

        mMessenger.notify(new Close(code, reason));
    }


    /**
     * WebSockets ping received, default notifies master.
     *
     * @param payload Ping payload or null.
     */
    protected void onPing(byte[] payload) {

        mMessenger.notify(new Ping(payload));
    }


    /**
     * WebSockets pong received, default notifies master.
     *
     * @param payload Pong payload or null.
     */
    protected void onPong(byte[] payload) {

        mMessenger.notify(new Pong(payload));
    }


    /**
     * WebSockets text message received, default notifies master.
     * This will only be called when the option receiveTextMessagesRaw
     * HAS NOT been set.
     *
     * @param payload Text message payload as Java String decoded
     *                from raw UTF-8 payload or null (empty payload).
     */
    protected void onTextMessage(String payload) {

        mMessenger.notify(new TextMessage(payload));
    }


    /**
     * WebSockets text message received, default notifies master.
     * This will only be called when the option receiveTextMessagesRaw
     * HAS been set.
     *
     * @param payload Text message payload as raw UTF-8 octets or
     *                null (empty payload).
     */
    protected void onRawTextMessage(byte[] payload) {

        mMessenger.notify(new RawTextMessage(payload));
    }


    /**
     * WebSockets binary message received, default notifies master.
     *
     * @param payload Binary message payload or null (empty payload).
     */
    protected void onBinaryMessage(byte[] payload) {

        mMessenger.notify(new BinaryMessage(payload));
    }

    private void emitServerError(String msg) {
        mMessenger.notify(new ServerError(msg));
        mState = STATE_CLOSED;
        mStopped = true;
    }

    /**
     * Process WebSockets handshake received from server.
     */
    private boolean processHandshake() throws UnsupportedEncodingException {

        boolean res = false;
        for (int pos = mPosition - 4; pos >= 0; --pos) {
            if (mMessageData[pos] == 0x0d && mMessageData[pos + 1] == 0x0a &&
                    mMessageData[pos + 2] == 0x0d && mMessageData[pos + 3] == 0x0a) {

                // Check HTTP status code
                boolean serverError = false;
                String rawHeaders = new String(Arrays.copyOf(mMessageData, pos + 4), "UTF-8");
                String[] headers = rawHeaders.split("\r\n");

                String httpStatusLine = headers[0];
                String[] sl = httpStatusLine.split(" ");
                if (sl.length < 2 || !sl[0].startsWith("HTTP")) {
                    String msg = String.format("Bad HTTP response status line %s", httpStatusLine);
                    emitServerError(msg);
                    break;
                }

                String httpVersion = sl[0].trim();
                if (!httpVersion.equals("HTTP/1.1")) {
                    String msg = String.format("Unsupported HTTP version %s", httpVersion);
                    emitServerError(msg);
                    break;
                }

                int statusCode;

                try {
                    statusCode = Integer.parseInt(sl[1].trim());
                } catch (NumberFormatException ignore) {
                    String msg = String.format("Bad HTTP status code ('%s')", sl[1].trim());
                    emitServerError(msg);
                    break;
                }

                if (statusCode != 101) {
                    String reason = "";
                    if (sl.length > 2) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 2; i < sl.length; i++) {
                            builder.append(sl[i]);
                            // Don't add empty space at the end.
                            if (i != sl.length - 1) {
                                builder.append(" ");
                            }
                        }
                        reason = builder.toString();
                    }
                    String msg = String.format("WebSocket connection upgrade failed (%d %s)",
                            statusCode, reason);
                    emitServerError(msg);
                    break;
                }

                Map<String, String> handshakeParams = parseHttpHeaders(Arrays.copyOfRange(headers, 1, headers.length));

                if (!handshakeParams.containsKey("upgrade")) {
                    emitServerError("HTTP Upgrade header missing");
                    break;
                }

                String upgrade = handshakeParams.get("upgrade");
                if (upgrade == null || upgrade.toLowerCase().equals("upgrade")) {
                    String msg = String.format("HTTP Upgrade header different from 'websocket' " +
                            "(case-insensitive) : %s", upgrade);
                    emitServerError(msg);
                    break;
                }

                if (!handshakeParams.containsKey("connection")) {
                    emitServerError("HTTP Connection header missing");
                    break;
                }

                String[] connValues = handshakeParams.get("connection").split(",");
                boolean connectionUpgrade = false;
                for (String connValue : connValues) {
                    if (connValue.toLowerCase().equals("upgrade")) {
                        connectionUpgrade = true;
                        break;
                    }
                }

                if (!connectionUpgrade) {
                    String msg = String.format("HTTP Connection header does not include 'upgrade'" +
                            " value (case-insensitive) : %s", handshakeParams.get("connection"));
                    emitServerError(msg);
                    break;
                }

                if (!handshakeParams.containsKey("sec-websocket-accept")) {
                    String msg = "HTTP Sec-WebSocket-Accept header missing in opening handshake " +
                            "reply";
                    emitServerError(msg);
                    break;
                } else {
                    // FIXME: check sec-websocket-accept wasn't returned multiple times
                    // FIXME: verify websocket key
                }

                System.arraycopy(mMessageData, pos + 4, mMessageData, 0, mMessageData.length - (pos + 4));
                mPosition -= pos + 4;

                // process further when data after HTTP headers left in buffer
                res = mPosition > 0;
                mState = STATE_OPEN;

                onHandshake(handshakeParams);
                break;
            }
        }
        return res;
    }

    private Map<String, String> parseHttpHeaders(String[] httpResponse) {
        Map<String, String> headers = new HashMap<>();
        for (String line: httpResponse) {
            if (line.length() > 0) {
                String[] h = line.split(": ");
                if (h.length == 2) {
                    headers.put(h[0].toLowerCase(), h[1]);
                    LOGGER.d(String.format("'%s'='%s'", h[0].toLowerCase(), h[1]));
                }
            }
        }

        return headers;
    }

    /**
     * Consume data buffered in mFrameBuffer.
     */
    private boolean consumeData() throws Exception {

        if (mState == STATE_OPEN || mState == STATE_CLOSING) {

            return processData();

        } else if (mState == STATE_CONNECTING) {

            return processHandshake();

        } else if (mState == STATE_CLOSED) {

            return false;

        } else {
            // should not arrive here
            return false;
        }

    }


    /**
     * Run the background reader thread loop.
     */
    @Override
    public void run() {

        LOGGER.d("Running");

        try {
            do {
                // blocking read on socket
                int len = mBufferedStream.read(mMessageData, mPosition, mMessageData.length - mPosition);
                mPosition += len;
                if (len > 0) {

                    mLastReadTime = System.currentTimeMillis();
                    // process buffered data
                    while (consumeData()) {

                    }

                } else if (mState == STATE_CLOSED) {

                    mStopped = true;

                } else if (len < 0) {

                    LOGGER.d("run() : ConnectionLost");

                    mMessenger.notify(new ConnectionLost(null));

                    mStopped = true;
                }
            } while (!mStopped);

        } catch (WebSocketException e) {

            LOGGER.d("run() : WebSocketException (" + e.toString() + ")");

            // wrap the exception and notify master
            mMessenger.notify(new ProtocolViolation(e));

        } catch (SocketException e) {

            // BufferedInputStream throws when the socket is closed,
            // eat the exception if we are already in STATE_CLOSED.
            if (mState != STATE_CLOSED && !mSocket.isClosed()) {
                LOGGER.d("run() : SocketException (" + e.toString() + ")");

                // wrap the exception and notify master
                mMessenger.notify(new ConnectionLost(null));
            }

        } catch (Exception e) {

            LOGGER.d("run() : Exception (" + e.toString() + ")");

            // wrap the exception and notify master
            mMessenger.notify(new Error(e));

        } finally {

            mStopped = true;
        }

        LOGGER.d("Ended");
    }
}
