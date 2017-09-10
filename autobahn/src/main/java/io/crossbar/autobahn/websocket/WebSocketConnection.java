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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IWebSocket;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.messages.BinaryMessage;
import io.crossbar.autobahn.websocket.messages.ClientHandshake;
import io.crossbar.autobahn.websocket.messages.Close;
import io.crossbar.autobahn.websocket.messages.ConnectionLost;
import io.crossbar.autobahn.websocket.messages.Error;
import io.crossbar.autobahn.websocket.messages.Ping;
import io.crossbar.autobahn.websocket.messages.Pong;
import io.crossbar.autobahn.websocket.messages.ProtocolViolation;
import io.crossbar.autobahn.websocket.messages.Quit;
import io.crossbar.autobahn.websocket.messages.RawTextMessage;
import io.crossbar.autobahn.websocket.messages.ServerError;
import io.crossbar.autobahn.websocket.messages.ServerHandshake;
import io.crossbar.autobahn.websocket.messages.TextMessage;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;


public class WebSocketConnection implements IWebSocket {

    private static final boolean DEBUG = true;
    private static final String TAG = WebSocketConnection.class.getName();

    private Handler mMasterHandler;

    private WebSocketReader mReader;
    private WebSocketWriter mWriter;
    private HandlerThread mWriterThread;

    private Socket mSocket;
    private URI mWsUri;
    private String mWsScheme;
    private String mWsHost;
    private int mWsPort;
    private String mWsPath;
    private String mWsQuery;
    private String[] mWsSubprotocols;
    private Map<String, String> mWsHeaders;
    private Map<String, String> mHandshakeHeaders;

    private IWebSocketConnectionHandler mWsHandler;

    private WebSocketOptions mOptions;

    private boolean mActive;
    private boolean mPrevConnected;
    private boolean onCloseCalled;

    /**
     * Asynchronous socket connector.
     */
    private class WebSocketConnector extends Thread {

        public void run() {
            Thread.currentThread().setName("WebSocketConnector");

			/*
             * connect TCP socket
			 */
            try {
                if (mWsScheme.equals("wss")) {
                    mSocket = SSLSocketFactory.getDefault().createSocket();
                } else {
                    mSocket = SocketFactory.getDefault().createSocket();
                }

                // the following will block until connection was established or
                // an error occurred!
                mSocket.connect(new InetSocketAddress(mWsHost, mWsPort), mOptions.getSocketConnectTimeout());

                // before doing any data transfer on the socket, set socket
                // options
                mSocket.setSoTimeout(mOptions.getSocketReceiveTimeout());
                mSocket.setTcpNoDelay(mOptions.getTcpNoDelay());

            } catch (IOException e) {
                onClose(IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT,
                        e.getMessage());
                return;
            }

            if (isConnected()) {

                try {

                    // create & start WebSocket reader
                    createReader();

                    // create & start WebSocket writer
                    createWriter();

                    // start WebSockets handshake
                    ClientHandshake hs = new ClientHandshake(
                            mWsHost + ":" + mWsPort);
                    hs.mPath = mWsPath;
                    hs.mQuery = mWsQuery;
                    hs.mSubprotocols = mWsSubprotocols;
                    hs.mHeaderList = mWsHeaders;
                    mWriter.forward(hs);

                    mPrevConnected = true;

                } catch (Exception e) {
                    onClose(IWebSocketConnectionHandler.CLOSE_INTERNAL_ERROR,
                            e.getMessage());
                }
            } else {
                onClose(IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT,
                        "Could not connect to WebSocket server");
            }
        }
    }

    public WebSocketConnection() {
        if (DEBUG) Log.d(TAG, "created");

        // create WebSocket master handler
        createHandler();

        // set initial values
        mActive = false;
        mPrevConnected = false;
    }

    @Override
    public void sendMessage(String payload) {
        mWriter.forward(new TextMessage(payload));
    }

    @Override
    public void sendMessage(byte[] payload, boolean isBinary) {
        if (isBinary) {
            mWriter.forward(new BinaryMessage(payload));
        } else {
            mWriter.forward(new RawTextMessage(payload));
        }
    }

    @Override
    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected() && !mSocket.isClosed();
    }

    private void closeReaderThread(boolean waitForQuit) {
        if (mReader != null) {
            mReader.quit();
            if (waitForQuit) {
                try {
                    mReader.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (DEBUG) Log.d(TAG, "mReader already NULL");
        }
    }

    private void closeUnderlyingSocket() throws IOException, InterruptedException {
        Thread cleaner = new Thread(() -> {
            if (isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cleaner.start();
        cleaner.join();
    }

    private void closeWriterThread() {
        if (mWriter != null) {
            //mWriterThread.getLooper().quit();
            mWriter.forward(new Quit());
            try {
                mWriterThread.join();
            } catch (InterruptedException e) {
                if (DEBUG) e.printStackTrace();
            }
            //mWriterThread = null;
        } else {
            if (DEBUG) Log.d(TAG, "mWriter already NULL");
        }
    }


    private void failConnection(int code, String reason) {
        if (DEBUG) Log.d(TAG, "fail connection [code = " + code + ", reason = " + reason);

        closeReaderThread(false);

        closeWriterThread();

        if (isConnected()) {
            try {
                closeUnderlyingSocket();
            } catch (IOException | InterruptedException e) {
                if (DEBUG) e.printStackTrace();
            }
        } else {
            if (DEBUG) Log.d(TAG, "mTransportChannel already NULL");
        }

        closeReaderThread(true);

        onClose(code, reason);

        if (DEBUG) Log.d(TAG, "worker threads stopped");
    }

    @Override
    public void connect(String wsUri, IWebSocketConnectionHandler wsHandler)
            throws WebSocketException {
        connect(wsUri, null, wsHandler, new WebSocketOptions(), null);
    }

    @Override
    public void connect(String wsUri, IWebSocketConnectionHandler wsHandler,
                        WebSocketOptions options) throws WebSocketException {
        connect(wsUri, null, wsHandler, options, null);
    }

    @Override
    public void connect(String wsUri, String[] wsSubprotocols,
                        IWebSocketConnectionHandler wsHandler) throws WebSocketException {
        connect(wsUri, wsSubprotocols, wsHandler, new WebSocketOptions(), null);
    }

    @Override
    public void connect(String wsUri, String[] wsSubprotocols,
                        IWebSocketConnectionHandler wsHandler, WebSocketOptions options,
                        Map<String, String> headers) throws WebSocketException {

        // don't connect if already connected .. user needs to disconnect first
        //
        if (isConnected()) {
            throw new WebSocketException("already connected");
        }

        // parse WebSockets URI
        //
        try {
            mWsUri = new URI(wsUri);

            if (!mWsUri.getScheme().equals("ws") && !mWsUri.getScheme().equals("wss")) {
                throw new WebSocketException("unsupported scheme for WebSockets URI");
            }

            mWsScheme = mWsUri.getScheme();

            if (mWsUri.getPort() == -1) {
                if (mWsScheme.equals("ws")) {
                    mWsPort = 80;
                } else {
                    mWsPort = 443;
                }
            } else {
                mWsPort = mWsUri.getPort();
            }

            if (mWsUri.getHost() == null) {
                throw new WebSocketException("no host specified in WebSockets URI");
            } else {
                mWsHost = mWsUri.getHost();
            }

            if (mWsUri.getRawPath() == null || mWsUri.getRawPath().equals("")) {
                mWsPath = "/";
            } else {
                mWsPath = mWsUri.getRawPath();
            }

            if (mWsUri.getRawQuery() == null || mWsUri.getRawQuery().equals("")) {
                mWsQuery = null;
            } else {
                mWsQuery = mWsUri.getRawQuery();
            }

        } catch (URISyntaxException e) {
            throw new WebSocketException("invalid WebSockets URI");
        }

        mWsSubprotocols = wsSubprotocols;
        mWsHeaders = headers;
        mWsHandler = wsHandler;

        // make copy of options!
        mOptions = new WebSocketOptions(options);

        // set connection active
        mActive = true;

        // reset value
        onCloseCalled = false;

        // use async connector on short-lived background thread
        new WebSocketConnector().start();
    }

    @Override
    public void sendClose() {
        sendClose(1000);
    }

    @Override
    public void sendClose(int code) {
        sendClose(code, null);
    }

    @Override
    public void sendClose(int code, String reason) {
        // Close the writer thread here but delay the closing of reader thread
        // as we need to have active connection to be able to process the response
        // of this close request.
        if (mWriter != null) {
            mWriter.forward(new Close(code, reason));
        } else {
            if (DEBUG) Log.d(TAG, "could not send Close .. writer already NULL");
        }
        onCloseCalled = false;
        mActive = false;
        mPrevConnected = false;
    }

    public Map<String, String> getHandshakeResponseHeaders() {
        return mHandshakeHeaders;
    }

    /**
     * Reconnect to the server with the latest options
     *
     * @return true if reconnection performed
     */
    public boolean reconnect() {
        if (!isConnected() && (mWsUri != null)) {
            new WebSocketConnector().start();
            return true;
        }
        return false;
    }

    /**
     * Perform reconnection
     *
     * @return true if reconnection was scheduled
     */
    private boolean scheduleReconnect() {
        /**
         * Reconnect only if:
         *  - connection active (connected but not disconnected)
         *  - has previous success connections
         *  - reconnect interval is set
         */
        int interval = mOptions.getReconnectInterval();
        boolean need = mActive && mPrevConnected && (interval > 0);
        if (need) {
            if (DEBUG) Log.d(TAG, "Reconnection scheduled");
            mMasterHandler.postDelayed(() -> {
                if (DEBUG) Log.d(TAG, "Reconnecting...");
                reconnect();
            }, interval);
        }
        return need;
    }

    /**
     * Common close handler
     *
     * @param code   Close code.
     * @param reason Close reason (human-readable).
     */
    private void onClose(int code, String reason) {
        boolean reconnecting = false;

        if ((code == IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT) ||
                (code == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST)) {
            reconnecting = scheduleReconnect();
        }


        if (mWsHandler != null) {
            try {
                if (reconnecting) {
                    mWsHandler.onClose(IWebSocketConnectionHandler.CLOSE_RECONNECT, reason);
                } else {
                    mWsHandler.onClose(code, reason);
                }
            } catch (Exception e) {
                if (DEBUG) e.printStackTrace();
            }
            //mWsHandler = null;
        } else {
            if (DEBUG) Log.d(TAG, "mWsHandler already NULL");
        }
        onCloseCalled = true;
    }

    private void closeAndCleanup() {
        // Close the reader thread but don't wait for it to quit because
        // the blocking call to BufferedInputStream.read() can take a
        // a few seconds in some cases to unblock. We call this method later
        // a few lines below *after* we close the socket, because as soon as
        // the Socket.close() is called, BufferedInputStream.read() throws.
        // So this gives us quick cleaning of resources.
        closeReaderThread(false);
        closeWriterThread();
        if (isConnected()) {
            try {
                closeUnderlyingSocket();
            } catch (IOException | InterruptedException e) {
                if (DEBUG) e.printStackTrace();
            }
        }
        closeReaderThread(true);
        onCloseCalled = false;
    }


    /**
     * Create master message handler.
     */
    private void createHandler() {

        mMasterHandler = new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {
                // We have received the closing handshake and replied to it, discard
                // anything received after that.
                if (onCloseCalled) {
                    if (DEBUG) Log.d(TAG, "onClose called already, ignore message.");
                    return;
                }

                if (msg.obj instanceof TextMessage) {

                    TextMessage textMessage = (TextMessage) msg.obj;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(textMessage.mPayload);
                    } else {
                        if (DEBUG) Log.d(TAG, "could not call onTextMessage() .. handler already NULL");
                    }

                } else if (msg.obj instanceof RawTextMessage) {

                    RawTextMessage rawTextMessage = (RawTextMessage) msg.obj;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(rawTextMessage.mPayload, false);
                    } else {
                        if (DEBUG) Log.d(TAG, "could not call onRawTextMessage() .. handler already NULL");
                    }

                } else if (msg.obj instanceof BinaryMessage) {

                    BinaryMessage binaryMessage = (BinaryMessage) msg.obj;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(binaryMessage.mPayload, true);
                    } else {
                        if (DEBUG) Log.d(TAG, "could not call onBinaryMessage() .. handler already NULL");
                    }

                } else if (msg.obj instanceof Ping) {

                    Ping ping = (Ping) msg.obj;
                    if (DEBUG) Log.d(TAG, "WebSockets Ping received");

                    // reply with Pong
                    Pong pong = new Pong();
                    pong.mPayload = ping.mPayload;
                    mWriter.forward(pong);

                } else if (msg.obj instanceof Pong) {

                    @SuppressWarnings("unused")
                    Pong pong = (Pong) msg.obj;

                    if (DEBUG) Log.d(TAG, "WebSockets Pong received");

                } else if (msg.obj instanceof Close) {

                    Close close = (Close) msg.obj;

                    final int crossbarCloseCode = (close.mCode == 1000) ? IWebSocketConnectionHandler.CLOSE_NORMAL : IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST;

                    if (close.mIsReply) {
                        if (DEBUG) Log.d(TAG, "WebSockets Close received (" + close.mCode + " - " + close.mReason + ")");
                        closeAndCleanup();
                        onClose(crossbarCloseCode, close.mReason);
                    } else if (mActive) {
                        // We have received a close frame, lets clean.
                        closeReaderThread(false);
                        mWriter.forward(new Close(1000, true));
                        mActive = false;
                    } else {
                        if (DEBUG) Log.d(TAG, "WebSockets Close received (" + close.mCode + " - " + close.mReason + ")");
                        // we've initiated disconnect, so ready to close the channel
                        closeAndCleanup();
                        onClose(crossbarCloseCode, close.mReason);
                    }

                } else if (msg.obj instanceof ServerHandshake) {

                    ServerHandshake serverHandshake = (ServerHandshake) msg.obj;

                    if (DEBUG) Log.d(TAG, "opening handshake received");

                    mHandshakeHeaders = serverHandshake.headers;

                    if (serverHandshake.mSuccess) {
                        if (mWsHandler != null) {
                            mWsHandler.onOpen();
                            if (DEBUG) Log.d(TAG, "onOpen() called, ready to rock.");
                        } else {
                            if (DEBUG) Log.d(TAG, "could not call onOpen() .. handler already NULL");
                        }
                    }

                } else if (msg.obj instanceof ConnectionLost) {

                    @SuppressWarnings("unused")
                    ConnectionLost connnectionLost = (ConnectionLost) msg.obj;
                    failConnection(IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST, "WebSockets connection lost");

                } else if (msg.obj instanceof ProtocolViolation) {

                    @SuppressWarnings("unused")
                    ProtocolViolation protocolViolation = (ProtocolViolation) msg.obj;
                    failConnection(IWebSocketConnectionHandler.CLOSE_PROTOCOL_ERROR, "WebSockets protocol violation");

                } else if (msg.obj instanceof Error) {

                    Error error = (Error) msg.obj;
                    failConnection(IWebSocketConnectionHandler.CLOSE_INTERNAL_ERROR, "WebSockets internal error (" + error.mException.toString() + ")");

                } else if (msg.obj instanceof ServerError) {

                    ServerError error = (ServerError) msg.obj;
                    failConnection(IWebSocketConnectionHandler.CLOSE_SERVER_ERROR, "Server error " + error.mStatusCode + " (" + error.mStatusMessage + ")");

                } else {

                    processAppMessage(msg.obj);

                }
            }
        };
    }


    private void processAppMessage(Object message) {
    }


    /**
     * Create WebSockets background writer.
     */
    private void createWriter() throws IOException {

        mWriterThread = new HandlerThread("WebSocketWriter");
        mWriterThread.start();
        mWriter = new WebSocketWriter(mWriterThread.getLooper(), mMasterHandler, mSocket, mOptions);

        if (DEBUG) Log.d(TAG, "WS writer created and started");
    }


    /**
     * Create WebSockets background reader.
     */
    private void createReader() throws IOException {

        mReader = new WebSocketReader(mMasterHandler, mSocket, mOptions, "WebSocketReader");
        mReader.start();

        if (DEBUG) Log.d(TAG, "WS reader created and started");
    }
}
