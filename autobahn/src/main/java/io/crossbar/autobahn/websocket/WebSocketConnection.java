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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.websocket.exceptions.ParseFailed;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;
import io.crossbar.autobahn.websocket.interfaces.IWebSocket;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.messages.BinaryMessage;
import io.crossbar.autobahn.websocket.messages.CannotConnect;
import io.crossbar.autobahn.websocket.messages.ClientHandshake;
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
import io.crossbar.autobahn.websocket.types.ConnectionResponse;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;

import static io.crossbar.autobahn.websocket.utils.Platform.selectThreadMessenger;

public class WebSocketConnection implements IWebSocket {

    private static final IABLogger LOGGER = ABLogger.getLogger(WebSocketConnection.class.getName());

    private IThreadMessenger mMessenger;

    private WebSocketReader mReader;
    private ExecutorService mWriterThread;
    private Connection mWebSocket;
    private BufferedOutputStream mBufferedOutputStream;

    private Socket mSocket;
    private URI mWsUri;
    private String mWsScheme;
    private String mWsHost;
    private int mWsPort;
    private String mWsPath;
    private String mWsQuery;
    private String[] mWsSubprotocols;
    private Map<String, String> mWsHeaders;

    private IWebSocketConnectionHandler mWsHandler;

    private WebSocketOptions mOptions;

    private boolean mActive;
    private boolean mPrevConnected;
    private boolean onCloseCalled;

    private ScheduledExecutorService mExecutor;
    private ScheduledFuture<?> mPingerTask;
    private ScheduledFuture<?> mTimeoutTask;

    private final Runnable mAutoPinger = new Runnable() {
        @Override
        public void run() {
            if (!isConnected()) {
                return;
            }

            if (mReader.getTimeSinceLastRead() >= mOptions.getAutoPingInterval()) {
                sendPing();

                mTimeoutTask = mExecutor.schedule(() -> {

                    if (!isConnected()) {
                        return;
                    }

                    // We didn't receive a WebSocket read, something is likely wrong there.
                    if (mReader.getTimeSinceLastRead() >= mOptions.getAutoPingTimeout()) {
                        mMessenger.notify(new ConnectionLost("WebSocket ping timed out."));
                    }
                }, mOptions.getAutoPingTimeout(), TimeUnit.SECONDS);
            }
        }
    };

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

                if (mOptions.getTLSEnabledProtocols() != null){
                    setEnabledProtocolsOnSSLSocket(mSocket, mOptions.getTLSEnabledProtocols());
                }

                // the following will block until connection was established or
                // an error occurred!
                mSocket.connect(new InetSocketAddress(mWsHost, mWsPort),
                        mOptions.getSocketConnectTimeout());

                // before doing any data transfer on the socket, set socket
                // options
                mSocket.setSoTimeout(mOptions.getSocketReceiveTimeout());
                mSocket.setTcpNoDelay(mOptions.getTcpNoDelay());

            } catch (IOException e) {
                mMessenger.notify(new CannotConnect(e.getMessage()));
                return;
            }

            if (mExecutor == null || mExecutor.isShutdown()) {
                mExecutor = Executors.newSingleThreadScheduledExecutor();
            }

            if (isConnected()) {

                try {

                    // create & start WebSocket reader
                    createReader();

                    // create & start WebSocket writer
                    createWriter();

                    // start WebSocket handshake
                    ClientHandshake hs = new ClientHandshake(mWsHost + ":" + mWsPort);
                    hs.mPath = mWsPath;
                    hs.mQuery = mWsQuery;
                    hs.mSubprotocols = mWsSubprotocols;
                    hs.mHeaderList = mWsHeaders;
                    sendMessage(hs);
                    mPrevConnected = true;

                } catch (Exception e) {
                    mMessenger.notify(new Error(e));
                }
            } else {
                mMessenger.notify(new CannotConnect("Could not connect to WebSocket server"));
            }
        }
    }

    public WebSocketConnection() {
        LOGGER.d("Created");

        // create WebSocket master handler
        createHandler();

        // set initial values
        mActive = false;
        mPrevConnected = false;
    }

    @Override
    public void sendMessage(String payload) {
        sendMessage(new TextMessage(payload));
    }

    @Override
    public void sendMessage(byte[] payload, boolean isBinary) {
        if (isBinary) {
            sendMessage(new BinaryMessage(payload));
        } else {
            sendMessage(new RawTextMessage(payload));
        }
    }

    @Override
    public void sendPing() {
        sendMessage(new Ping());
        LOGGER.d("WebSocket Ping sent");
    }

    @Override
    public void sendPing(byte[] payload) {
        sendMessage(new Ping(payload));
    }

    @Override
    public void sendPong() {
        sendMessage(new Pong());
        LOGGER.d("WebSocket Pong sent");
    }

    @Override
    public void sendPong(byte[] payload) {
        sendMessage(new Pong(payload));
        LOGGER.d("WebSocket Pong sent");
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
                    LOGGER.v(e.getMessage(), e);
                }
            }
        } else {
            LOGGER.d("mReader already NULL");
        }
    }

    private void closeUnderlyingSocket() throws InterruptedException {
        Thread cleaner = new Thread(() -> {
            if (isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    LOGGER.v(e.getMessage(), e);
                }
            }
        });
        cleaner.start();
        cleaner.join();
    }

    private void closeWriterThread() {
        if (mWriterThread != null) {
            try {
                mWriterThread.shutdown();
                mWriterThread.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.v(e.getMessage(), e);
            }
        }
    }

    private void failConnection(int code, String reason) {
        LOGGER.d("fail connection [code = " + code + ", reason = " + reason);

        closeReaderThread(false);

        closeWriterThread();

        if (isConnected()) {
            try {
                closeUnderlyingSocket();
            } catch (InterruptedException e) {
                LOGGER.v(e.getMessage(), e);
            }
        } else {
            LOGGER.d("Socket already closed");
        }

        closeReaderThread(true);

        onClose(code, reason);

        LOGGER.d("Worker threads stopped");
    }

    @Override
    public void connect(String wsUri, IWebSocketConnectionHandler wsHandler)
            throws WebSocketException {
        connect(wsUri, null, wsHandler, null, null);
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

        // parse WebSocket URI
        //
        try {
            mWsUri = new URI(wsUri);

            mWsScheme = mWsUri.getScheme();
            if (mWsScheme == null || (!mWsScheme.equals("ws") && !mWsScheme.equals("wss"))) {
                throw new WebSocketException("unsupported scheme for WebSocket URI");
            }

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
                throw new WebSocketException("no host specified in WebSocket URI");
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
            throw new WebSocketException("invalid WebSocket URI");
        }

        mWsSubprotocols = wsSubprotocols;
        mWsHeaders = headers;
        mWsHandler = wsHandler;

        if (mOptions == null) {
            if (options == null) {
                mOptions = new WebSocketOptions();
            } else {
                // make copy of options!
                mOptions = new WebSocketOptions(options);
            }
        // WebSocketOptions were already, replace them if an instance was provided
        // with connect() as well.
        } else if (options != null) {
            mOptions = new WebSocketOptions(options);
        }

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
        sendMessage(new Close(code, reason));
        onCloseCalled = false;
        mActive = false;
        mPrevConnected = false;
    }

    /**
     * Reconnect to the server with the latest options
     *
     * @return true if reconnection performed
     */
    public boolean reconnect() {
        if (!isConnected() && (mWsUri != null)) {
            onCloseCalled = false;
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
            LOGGER.d("Reconnection scheduled");
            mMessenger.postDelayed(() -> {
                LOGGER.d("Reconnecting...");
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

        // Shutdown the executor so that it stops attempting to send pings.
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
        mMessenger.cleanup();

        if (mWsHandler != null) {
            try {
                if (reconnecting) {
                    mWsHandler.onClose(IWebSocketConnectionHandler.CLOSE_RECONNECT, reason);
                } else {
                    mWsHandler.onClose(code, reason);
                }
            } catch (Exception e) {
                LOGGER.v(e.getMessage(), e);
            }
            //mWsHandler = null;
        } else {
            LOGGER.d("mWsHandler already NULL");
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
            } catch (InterruptedException e) {
                LOGGER.v(e.getMessage(), e);
            }
        }
        closeReaderThread(true);
        onCloseCalled = false;
    }

    private <T> T getOrDefault(Map<?, ?> obj, Object key, T default_value) {
        if (obj.containsKey(key)) {
            return (T) obj.get(key);
        }
        return default_value;
    }

    public void setOptions(WebSocketOptions options) {
        boolean optionsWasNull = mOptions == null;
        if (mOptions == null) {
            mOptions = new WebSocketOptions(options);
        } else {
            mOptions.setAutoPingInterval(options.getAutoPingInterval());
            mOptions.setAutoPingTimeout(options.getAutoPingTimeout());
        }

        if (!optionsWasNull) {
            // Now do the magic here.
            if (mPingerTask != null) {
                mPingerTask.cancel(true);
            }
            if (mExecutor == null) {
                mExecutor = Executors.newSingleThreadScheduledExecutor();
            }
            if (isConnected() && mOptions.getAutoPingInterval() > 0) {
                mPingerTask = mExecutor.scheduleAtFixedRate(
                        mAutoPinger, mOptions.getAutoPingInterval(),
                        mOptions.getAutoPingInterval(), TimeUnit.SECONDS);
            }
        }
    }


    /**
     * Create master message handler.
     */
    private void createHandler() {
        mMessenger = selectThreadMessenger();
        mMessenger.setOnMessageListener(new IThreadMessenger.OnMessageListener() {
            @Override
            public void onMessage(Object message) {
                // We have received the closing handshake and replied to it, discard
                // anything received after that.
                if (onCloseCalled) {
                    LOGGER.d("onClose called already, ignore message.");
                    return;
                }

                if (message instanceof TextMessage) {

                    TextMessage textMessage = (TextMessage) message;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(textMessage.mPayload);
                    } else {
                        LOGGER.d("could not call onTextMessage() .. handler already NULL");
                    }

                } else if (message instanceof RawTextMessage) {

                    RawTextMessage rawTextMessage = (RawTextMessage) message;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(rawTextMessage.mPayload, false);
                    } else {
                        LOGGER.d("could not call onRawTextMessage() .. handler already NULL");
                    }

                } else if (message instanceof BinaryMessage) {

                    BinaryMessage binaryMessage = (BinaryMessage) message;

                    if (mWsHandler != null) {
                        mWsHandler.onMessage(binaryMessage.mPayload, true);
                    } else {
                        LOGGER.d("could not call onBinaryMessage() .. handler already NULL");
                    }

                } else if (message instanceof Ping) {

                    Ping ping = (Ping) message;
                    LOGGER.d("WebSocket Ping received");

                    if (ping.mPayload == null) {
                        mWsHandler.onPing();
                    } else {
                        mWsHandler.onPing(ping.mPayload);
                    }

                } else if (message instanceof Pong) {
                    Pong pong = (Pong) message;

                    if (pong.mPayload == null) {
                        mWsHandler.onPong();
                    } else {
                        mWsHandler.onPong(pong.mPayload);
                    }

                    // We already received a pong, cancel the timeout executor
                    if (mTimeoutTask != null && !mTimeoutTask.isDone() && !mTimeoutTask.isCancelled()) {
                        mTimeoutTask.cancel(true);
                    }

                    LOGGER.d("WebSocket Pong received");

                } else if (message instanceof Close) {

                    Close close = (Close) message;

                    final int crossbarCloseCode = (close.mCode == 1000) ? IWebSocketConnectionHandler.CLOSE_NORMAL : IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST;

                    if (close.mIsReply) {
                        LOGGER.d("WebSocket Close received (" + close.mCode + " - " + close.mReason + ")");
                        closeAndCleanup();
                        onClose(crossbarCloseCode, close.mReason);
                    } else if (mActive) {
                        // We have received a close frame, lets clean.
                        closeReaderThread(false);
                        WebSocketConnection.this.sendMessage(new Close(1000, true));
                        mActive = false;
                    } else {
                        LOGGER.d("WebSocket Close received (" + close.mCode + " - " + close.mReason + ")");
                        // we've initiated disconnect, so ready to close the channel
                        closeAndCleanup();
                        onClose(crossbarCloseCode, close.mReason);
                    }

                } else if (message instanceof ServerHandshake) {

                    ServerHandshake serverHandshake = (ServerHandshake) message;

                    LOGGER.d("opening handshake received");

                    if (mWsHandler != null) {
                        String protocol = getOrDefault(serverHandshake.headers,
                                "sec-websocket-protocol", null);
                        mWsHandler.setConnection(WebSocketConnection.this);
                        mWsHandler.onConnect(new ConnectionResponse(protocol));
                        mWsHandler.onOpen();
                        LOGGER.d("onOpen() called, ready to rock.");

                        if (mOptions.getAutoPingInterval() > 0) {
                            mPingerTask = mExecutor.scheduleAtFixedRate(
                                    mAutoPinger, mOptions.getAutoPingInterval(),
                                    mOptions.getAutoPingInterval(), TimeUnit.SECONDS);
                        }
                    } else {
                        LOGGER.d("could not call onOpen() .. handler already NULL");
                    }
                } else if (message instanceof CannotConnect) {

                    CannotConnect cannotConnect = (CannotConnect) message;
                    failConnection(IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT,
                            cannotConnect.reason);

                } else if (message instanceof ConnectionLost) {

                    ConnectionLost connnectionLost = (ConnectionLost) message;
                    failConnection(IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST,
                            connnectionLost.reason);

                } else if (message instanceof ProtocolViolation) {

                    failConnection(IWebSocketConnectionHandler.CLOSE_PROTOCOL_ERROR,
                            "WebSocket protocol violation");

                } else if (message instanceof Error) {

                    Error error = (Error) message;
                    failConnection(IWebSocketConnectionHandler.CLOSE_INTERNAL_ERROR,
                            "WebSocket internal error (" + error.mException.toString() + ")");

                } else if (message instanceof ServerError) {

                    ServerError error = (ServerError) message;
                    failConnection(IWebSocketConnectionHandler.CLOSE_SERVER_ERROR,
                            "Server error " + error.mStatusMessage);

                } else {

                    processAppMessage(message);

                }
            }
        });
    }


    private void processAppMessage(Object message) {
    }


    /**
     * Create WebSocket background writer.
     */
    private void createWriter() throws IOException {
        mWriterThread = Executors.newSingleThreadExecutor();
        mBufferedOutputStream = new BufferedOutputStream(mSocket.getOutputStream(),
                mOptions.getMaxFramePayloadSize() + 14);
        mWebSocket = new Connection(mOptions);
        LOGGER.d("WS writer created and started");
    }

    private void sendMessage(io.crossbar.autobahn.websocket.messages.Message message) {
        if (mWriterThread == null || mWriterThread.isShutdown()) {
            return;
        }
        mWriterThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mBufferedOutputStream.write(mWebSocket.send(message));
                    mBufferedOutputStream.flush();
                    if (message instanceof Close) {
                        Close msg = (Close) message;
                        if (msg.mIsReply) {
                            mMessenger.notify(message);
                        }
                    }
                } catch (SocketException e) {
                    LOGGER.d("run() : SocketException (" + e.toString() + ")");
                    mMessenger.notify(new ConnectionLost(null));
                } catch (ParseFailed | IOException e) {
                    LOGGER.w(e.getMessage(), e);
                    mMessenger.notify(new Error(e));
                }
            }
        });
    }

    /**
     * Create WebSocket background reader.
     */
    private void createReader() throws IOException {

        mReader = new WebSocketReader(mMessenger, mSocket, mOptions, "WebSocketReader");
        mReader.start();

        LOGGER.d("WS reader created and started");
    }

    /**
     * Enable protocols on SSLSocket.
     * @param socket
     * @param protocols
     */
    private void setEnabledProtocolsOnSSLSocket(Socket socket, String[] protocols) {
        if(socket != null && (socket instanceof SSLSocket)) {
            ((SSLSocket)socket).setEnabledProtocols(protocols);
        }
    }
}
