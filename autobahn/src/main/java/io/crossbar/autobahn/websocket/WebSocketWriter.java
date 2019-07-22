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
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.websocket.exceptions.ParseFailed;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.messages.Close;
import io.crossbar.autobahn.websocket.messages.ConnectionLost;
import io.crossbar.autobahn.websocket.messages.Error;
import io.crossbar.autobahn.websocket.messages.Message;
import io.crossbar.autobahn.websocket.messages.Quit;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;


/**
 * WebSocket writer, the sending leg of a WebSockets connection.
 * This is run on it's background thread with it's own message loop.
 * The only method that needs to be called (from foreground thread) is forward(),
 * which is used to forward a WebSockets message to this object (running on
 * background thread) so that it can be formatted and sent out on the
 * underlying TCP socket.
 */
class WebSocketWriter {

    private static final IABLogger LOGGER = ABLogger.getLogger(WebSocketWriter.class.getName());

    /// The send buffer that holds data to send on socket.
    private BufferedOutputStream mBufferedOutputStream;
    /// The tcp socket
    private Socket mSocket;
    /// Is Active.
    private boolean mActive;
    /// WebSocket state machinery
    private Connection mConnection;
    /// Event listener for master
    public interface onEventListener {
        void onEvent(Message msg);
    }
    private onEventListener mEventListener;
    private ExecutorService mWriterThread;


    /**
     * Create new WebSockets background writer.
     *
     * @param socket  The socket channel created on foreground thread.
     * @param options WebSockets connection options.
     */
    public WebSocketWriter(ExecutorService executorThread, Socket socket,
                           WebSocketOptions options) throws IOException {
        mWriterThread = executorThread;
        mSocket = socket;
        mBufferedOutputStream = new BufferedOutputStream(socket.getOutputStream(),
                options.getMaxFramePayloadSize() + 14);
        mConnection = new Connection(options);
        mActive = true;
        LOGGER.d("Created");
    }

    public void setOnEventListener(onEventListener listener) {
        mEventListener = listener;
    }

    public void write(Message msg) {
        mWriterThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (msg instanceof Quit) {
                        mActive = false;
                        LOGGER.d("Ended");
                    } else {
                        try {
                            mBufferedOutputStream.write(mConnection.send(msg));
                        } catch (ParseFailed parseFailed) {
                            throw new WebSocketException(parseFailed.getMessage());
                        }
                    }

                    // send out buffered data
                    if (mActive && mSocket.isConnected() && !mSocket.isClosed()) {
                        mBufferedOutputStream.flush();
                    }

                    // Check if the message that we sent was a close frame and was a reply
                    // to a closing handshake, if so, then notify master to close the socket.
                    if (msg instanceof Close) {
                        Close close = (Close) msg;
                        if (close.mIsReply) {
                            WebSocketWriter.this.notify(msg);
                        }
                    }
                } catch (SocketException e) {
                    LOGGER.d("run() : SocketException (" + e.toString() + ")");
                    // wrap the exception and notify master
                    WebSocketWriter.this.notify(new ConnectionLost(null));
                } catch (Exception e) {
                    LOGGER.w(e.getMessage(), e);
                    // wrap the exception and notify master
                    WebSocketWriter.this.notify(new Error(e));
                }
            }
        });
    }


    /**
     * Notify the master (foreground thread).
     *
     * @param message Message to send to master.
     */
    private void notify(Message message) {
        if (mEventListener != null) {
            mEventListener.onEvent(message);
        }
    }
}
