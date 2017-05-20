/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package io.crossbar.autobahn.wamp;

public interface Channel {

    void send(byte[] payload);

    interface ConnectionHandler {

        /**
         * Connection was closed normally.
         */
        int CLOSE_NORMAL = 1;

        /**
         * Connection could not be established in the first place.
         */
        int CLOSE_CANNOT_CONNECT = 2;

        /**
         * A previously established connection was lost unexpected.
         */
        int CLOSE_CONNECTION_LOST = 3;

        /**
         * The connection was closed because a protocol violation
         * occurred.
         */
        int CLOSE_PROTOCOL_ERROR = 4;

        /**
         * Internal error.
         */
        int CLOSE_INTERNAL_ERROR = 5;

        /**
         * Server returned error while connecting
         */
        int CLOSE_SERVER_ERROR = 6;

        /**
         * Server connection lost, scheduled reconnect
         */
        int CLOSE_RECONNECT = 7;

        /**
         * Fired when the WebSockets connection has been established.
         * After this happened, messages may be sent.
         */
        void onOpen();

        /**
         * Fired when the WebSockets connection has deceased (or could
         * not established in the first place).
         *
         * @param code   Close code.
         * @param reason Close reason (human-readable).
         */
        void onClose(int code, String reason);

        /**
         * Fired when a text message has been received (and text
         * messages are not set to be received raw).
         *
         * @param payload Text message payload or null (empty payload).
         */
        void onTextMessage(String payload);

        /**
         * Fired when a text message has been received (and text
         * messages are set to be received raw).
         *
         * @param payload Text message payload as raw UTF-8 or null (empty payload).
         */
        void onRawTextMessage(byte[] payload);

        /**
         * Fired when a binary message has been received.
         *
         * @param payload Binar message payload or null (empty payload).
         */
        void onBinaryMessage(byte[] payload);
    }
   interface MathOperation {
      int operation(int a, int b);
   }

   interface GreetingService {
      void sayMessage(String message);
   }

   private int operate(int a, int b, MathOperation mathOperation){
      return mathOperation.operation(a, b);
   }

    interface OnMessage {
        void operation(args, kwargs, details)
    }
    void on_message((args, kwargs, details) -> );
}


public class AuthRequest {
}

public class ChallengeResponseAuth extends AuthRequest {
}

public class CryptoSignAuth extends AuthRequest {
}

public class TicketAuth extends AuthRequest {
}

public class TlsClientAuth extends AuthRequest {
}

public class HttpCookieAuth extends AuthRequest {
}

public interface IObserver {
    void on(String event, )
}


public interface JoinedObserver {
    public void observeJoined(JoinedDetails details);
}



public class Subscription {

}

public class Registration {

}

public class CallResult {
    public object[] args;
    public ??? kwargs;
    public ?? details;
}

public class PublicationResult {
}


public class Session {

    void connect(Channel channel);

    Channel get_channel();

    void disconnect();

    int get_state() {
        // CONNECTED
        // CONNECTED_RESUMING
        // DISCONNECTED
        // DISCONNECTED_RESUMABLE
        // HELLO_SENT
        // AUTHENTICATE_SENT
        // JOINED
        // READY
        // GOODBYE_SENT
        // ABORT_SENT
    }

    /**
     * Publish an event to a topic.
     *
     * @param topic     The URI of the topic to publish on.
     * @param args      Positional arguments (payload) of the event publish.
     * @param kwargs    Keyword arguments (payload) of the event to publish.
     * @param options   Publication options.
     * @return          A CompletableFuture that resolves to an instance of Publication on success.
     */
    CompletableFuture<Publication> publish(String topic,
                                           List<Object> args,
                                           Map<String, Object> kwargs,
                                           PublishOptions options);

    CompletableFuture<Subscription> subscribe(String topic,
                                              SubscribeOptions options);

    /**
     * Issue a call to a procedure.
     *
     * @param procedure     The URI of the procedure to call.
     * @param args          Positional arguments (payload) to the call issued.
     * @param kwargs        Keyword arguments (payload) to the call issued.
     * @return              A CompletableFuture that resolves to an instance of CallResult on success.
     */
    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    CompletableFuture<Registration> register(String procedure,
                                             RegisterOptions options);



    // this is the list of observers that have registered their interest of
    // being notified when the "joined" state has been reached
    private final List<JoinedObserver> joined_observers = Lists.newCopyOnWriteArrayList();

    public void notify_on_joined(final JoinedObserver observer) {
        this.observers.add(observer);
    }

    // when a message channel has been connected to this session, the WAMP opening
    // handshake may be initiated from user code. doing so means providing a list of
    // authentication requests (which may include these: realm, authmethod, authid, authrole, authextra)
    CompletableFuture<SessionJoinedDetails> join(AuthRequest[] requests, AuthPolicy policy) {
        // asynchronously try to join using the given auth requests
        // according to the given auth/retry policy

        // assume, here the session has successfully joined a realm
        // using one of the auth requests succeeding

        // so now it's time to fire the user code that was registered
        // to be called when the "joined" state has been reached:
        for (final JoinedObserver observer : this.joined_observers) {
            joined_observer.observeJoined(details);
        }
    }


    private final List<LeftObserver> left_observers = Lists.newCopyOnWriteArrayList();

    public void notify_on_left(final LeftObserver observer) {
        this.left_observers.add(observer);
    }

    CompletableFuture<SessionLeftDetails> leave(String reason, String message) {
        for (final LeftObserver observer : this.left_observers) {
            observer.leftJoined(details);
        }
    }


    private final List<ReadyObserver> ready_observers = Lists.newCopyOnWriteArrayList();
}
