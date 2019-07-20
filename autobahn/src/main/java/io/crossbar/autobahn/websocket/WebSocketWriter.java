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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.websocket.exceptions.ParseFailed;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.messages.BinaryMessage;
import io.crossbar.autobahn.websocket.messages.ClientHandshake;
import io.crossbar.autobahn.websocket.messages.Close;
import io.crossbar.autobahn.websocket.messages.ConnectionLost;
import io.crossbar.autobahn.websocket.messages.Error;
import io.crossbar.autobahn.websocket.messages.Ping;
import io.crossbar.autobahn.websocket.messages.Pong;
import io.crossbar.autobahn.websocket.messages.Quit;
import io.crossbar.autobahn.websocket.messages.RawTextMessage;
import io.crossbar.autobahn.websocket.messages.TextMessage;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;


/**
 * WebSocket writer, the sending leg of a WebSockets connection.
 * This is run on it's background thread with it's own message loop.
 * The only method that needs to be called (from foreground thread) is forward(),
 * which is used to forward a WebSockets message to this object (running on
 * background thread) so that it can be formatted and sent out on the
 * underlying TCP socket.
 */
class WebSocketWriter extends Handler {

    private static final IABLogger LOGGER = ABLogger.getLogger(WebSocketWriter.class.getName());

    /// Connection master.
    private final Handler mMaster;

    /// Message looper this object is running on.
    private final Looper mLooper;

    /// WebSockets options.
    private final WebSocketOptions mOptions;

    /// The send buffer that holds data to send on socket.
    private BufferedOutputStream mBufferedOutputStream;

    /// The tcp socket
    private Socket mSocket;

    /// Is Active.
    private boolean mActive;

    private FrameProtocol mProtocol;


    /**
     * Create new WebSockets background writer.
     *
     * @param looper  The message looper of the background thread on which
     *                this object is running.
     * @param master  The message handler of master (foreground thread).
     * @param socket  The socket channel created on foreground thread.
     * @param options WebSockets connection options.
     */
    public WebSocketWriter(Looper looper, Handler master, Socket socket, WebSocketOptions options) throws IOException {

        super(looper);

        mLooper = looper;
        mMaster = master;
        mOptions = options;
        mSocket = socket;
        mBufferedOutputStream = new BufferedOutputStream(socket.getOutputStream(), options.getMaxFramePayloadSize() + 14);
        mActive = true;

        mProtocol = new FrameProtocol();

        LOGGER.d("Created");
    }

    /**
     * Call this from the foreground (UI) thread to make the writer
     * (running on background thread) send a WebSocket message on the
     * underlying TCP.
     *
     * @param message Message to send to WebSockets writer. An instance of a subclass of
     *                Message  or another type which then needs to be handled within
     *                processAppMessage() (in a class derived from this class).
     */
    public void forward(Object message) {
        // We have already quit, we are no longer sending messages.
        if (!mActive) {
            LOGGER.d("We have already quit, not processing further messages");
            return;
        }
        Message msg = obtainMessage();
        msg.obj = message;
        sendMessage(msg);
    }


    /**
     * Notify the master (foreground thread).
     *
     * @param message Message to send to master.
     */
    private void notify(Object message) {

        Message msg = mMaster.obtainMessage();
        msg.obj = message;
        mMaster.sendMessage(msg);
    }


    /**
     * Send WebSocket client handshake.
     */
    private void sendClientHandshake(ClientHandshake message) throws IOException {
        try {
            mBufferedOutputStream.write(Handshake.handshake(message));
        } catch (ParseFailed parseFailed) {
            throw new IOException(parseFailed.getMessage());
        }
    }


    /**
     * Send WebSockets close.
     */
    private void sendClose(Close message) throws IOException, WebSocketException {
        try {
            mBufferedOutputStream.write(mProtocol.close(message.mCode, message.mReason));
        } catch (ParseFailed parseFailed) {
            throw new WebSocketException(parseFailed.getMessage());
        }
    }


    /**
     * Send WebSockets ping.
     */
    private void sendPing(Ping message) throws IOException, WebSocketException {
        try {
            mBufferedOutputStream.write(mProtocol.ping(message.mPayload));
        } catch (ParseFailed parseFailed) {
            throw new WebSocketException(parseFailed.getMessage());
        }
    }


    /**
     * Send WebSockets pong. Normally, unsolicited Pongs are not used,
     * but Pongs are only send in response to a Ping from the peer.
     */
    private void sendPong(Pong message) throws IOException, WebSocketException {
        try {
            mBufferedOutputStream.write(mProtocol.pong(message.mPayload));
        } catch (ParseFailed parseFailed) {
            throw new WebSocketException(parseFailed.getMessage());
        }
        LOGGER.d("WebSockets Pong Sent");
    }


    /**
     * Send WebSockets binary message.
     */
    private void sendBinaryMessage(BinaryMessage message) throws IOException, WebSocketException {
        if (message.mPayload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        mBufferedOutputStream.write(mProtocol.sendBinary(message.mPayload));
    }


    /**
     * Send WebSockets text message.
     */
    private void sendTextMessage(TextMessage message) throws IOException, WebSocketException {
        byte[] payload = message.mPayload.getBytes("UTF-8");
        if (payload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        mBufferedOutputStream.write(mProtocol.sendText(payload));
    }


    /**
     * Send WebSockets binary message.
     */
    private void sendRawTextMessage(RawTextMessage message) throws IOException, WebSocketException {
        if (message.mPayload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        mBufferedOutputStream.write(mProtocol.sendText(message.mPayload));
    }


    /**
     * Process message received from foreground thread. This is called from
     * the message looper set up for the background thread running this writer.
     *
     * @param msg Message from thread message queue.
     */
    @Override
    public void handleMessage(Message msg) {

        try {

            // process message from master
            processMessage(msg.obj);

            // send out buffered data

            if (mActive && mSocket.isConnected() && !mSocket.isClosed()) {
                mBufferedOutputStream.flush();
            }

            // Check if the message that we sent was a close frame and was a reply
            // to a closing handshake, if so, then notify master to close the socket.
            if (msg.obj instanceof Close) {
                Close closeMessage = (Close) msg.obj;
                if (closeMessage.mIsReply) {
                    notify(new Close(closeMessage.mCode, closeMessage.mReason, true));
                }
            }


        } catch (SocketException e) {

            LOGGER.d("run() : SocketException (" + e.toString() + ")");

            // wrap the exception and notify master
            notify(new ConnectionLost(null));
        } catch (Exception e) {

            LOGGER.w(e.getMessage(), e);

            // wrap the exception and notify master
            notify(new Error(e));
        }
    }


    /**
     * Process WebSockets or control message from master. Normally,
     * there should be no reason to override this. If you do, you
     * need to know what you are doing.
     *
     * @param msg An instance of the Message subclass or a message
     *            that is handled in processAppMessage().
     */
    protected void processMessage(Object msg) throws IOException, WebSocketException {

        if (msg instanceof TextMessage) {

            sendTextMessage((TextMessage) msg);

        } else if (msg instanceof RawTextMessage) {

            sendRawTextMessage((RawTextMessage) msg);

        } else if (msg instanceof BinaryMessage) {

            sendBinaryMessage((BinaryMessage) msg);

        } else if (msg instanceof Ping) {

            sendPing((Ping) msg);

        } else if (msg instanceof Pong) {

            sendPong((Pong) msg);

        } else if (msg instanceof Close) {

            sendClose((Close) msg);

        } else if (msg instanceof ClientHandshake) {
            sendClientHandshake((ClientHandshake) msg);

        } else if (msg instanceof Quit) {

            mLooper.quit();
            mActive = false;

            LOGGER.d("Ended");

        } else {

            // call hook which may be overridden in derived class to process
            // messages we don't understand in this class
            processAppMessage(msg);
        }
    }


    /**
     * Process message other than plain WebSockets or control message.
     * This is intended to be overridden in derived classes.
     *
     * @param msg Message from foreground thread to process.
     */
    protected void processAppMessage(Object msg) throws WebSocketException, IOException {

        throw new WebSocketException("unknown message received by WebSocketWriter");
    }
}
