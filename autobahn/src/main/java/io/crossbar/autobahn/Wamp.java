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

package io.crossbar.autobahn;


import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.type.TypeReference;

import java.util.List;

/**
 * WAMP interface.
 */
public interface Wamp {

    public static final String URI_WAMP_BASE = "http://api.wamp.ws/";

    public static final String URI_WAMP_ERROR = URI_WAMP_BASE + "error#";

    public static final String URI_WAMP_PROCEDURE = URI_WAMP_BASE + "procedure#";

    public static final String URI_WAMP_TOPIC = URI_WAMP_BASE + "topic#";

    public static final String URI_WAMP_ERROR_GENERIC = URI_WAMP_ERROR + "generic";

    public static final String DESC_WAMP_ERROR_GENERIC = "generic error";

    public static final String URI_WAMP_ERROR_INTERNAL = URI_WAMP_ERROR + "internal";

    /**
     * Session handler for WAMP sessions.
     */
    public interface ConnectionHandler {

        /**
         * Fired upon successful establishment of connection to WAMP server.
         */
        public void onOpen();

        /**
         * Firex upon unsuccessful connection attempt or when connection
         * was closed normally, or abnormally.
         *
         * @param code   The close code, which provides information about why the connection was closed.
         * @param reason A humand readable description of the reason of connection close.
         */
        public void onClose(int code, String reason);
    }

    /**
     * Connect to WAMP server.
     *
     * @param wsUri          The WebSockets URI of the server.
     * @param sessionHandler The handler for the session.
     */
    public void connect(String wsUri, ConnectionHandler sessionHandler);

    /**
     * @param wsUri          The WebSockets URI of the server.
     * @param sessionHandler The handler for the session.
     * @param headers        The headers with the connection
     */
    public void connect(String wsUri, ConnectionHandler sessionHandler, List<BasicNameValuePair> headers);


    /**
     * Connect to WAMP server.
     *
     * @param wsUri          The WebSockets URI of the server.
     * @param sessionHandler The handler for the session.
     * @param options        WebSockets and Autobahn option.s
     * @param headers        Headers for connection
     */
    public void connect(String wsUri, ConnectionHandler sessionHandler, WampOptions options, List<BasicNameValuePair> headers);

    /**
     * Connect to WAMP server.
     *
     * @param wsUri          The WebSockets URI of the server.
     * @param sessionHandler The handler for the session.
     * @param options        WebSockets and Autobahn option.s
     */
    public void connect(String wsUri, ConnectionHandler sessionHandler, WampOptions options);

    /**
     * Disconnect from WAMP server.
     */
    public void disconnect();

    /**
     * Check if currently connected to server.
     *
     * @return True, iff connected.
     */
    public boolean isConnected();


    /**
     * Establish a prefix to be used in CURIEs to shorten URIs.
     *
     * @param prefix The prefix to be used in CURIEs.
     * @param uri    The full URI this prefix shall resolve to.
     */
    public void prefix(String prefix, String uri);

    /**
     * Call handler.
     */
    public interface CallHandler {

        /**
         * Fired on successful completion of call.
         *
         * @param result The RPC result transformed into the type that was specified in call.
         */
        public void onResult(Object result);

        /**
         * Fired on call failure.
         *
         * @param errorUri  The URI or CURIE of the error that occurred.
         * @param errorDesc A human readable description of the error.
         */
        public void onError(String errorUri, String errorDesc);
    }

    /**
     * Call a remote procedure (RPC).
     *
     * @param procUri     The URI or CURIE of the remote procedure to call.
     * @param resultType  The type the call result gets transformed into.
     * @param callHandler The handler to be invoked upon call completion.
     * @param arguments   Zero, one or more arguments for the call.
     */
    public void call(String procUri, Class<?> resultType, CallHandler callHandler, Object... arguments);

    /**
     * Call a remote procedure (RPC).
     *
     * @param procUri     The URI or CURIE of the remote procedure to call.
     * @param resultType  The type the call result gets transformed into.
     * @param callHandler The handler to be invoked upon call completion.
     * @param arguments   Zero, one or more arguments for the call.
     */
    public void call(String procUri, TypeReference<?> resultType, CallHandler callHandler, Object... arguments);

    /**
     * Handler for PubSub events.
     */
    public interface EventHandler {

        /**
         * Fired when an event for the PubSub subscription is received.
         *
         * @param topicUri The URI or CURIE of the topic the event was published to.
         * @param event    The event, transformed into the type that was specified when subscribing.
         */
        public void onEvent(String topicUri, Object event);
    }

    /**
     * Subscribe to a topic. When already subscribed, overwrite the event handler.
     *
     * @param topicUri     The URI or CURIE of the topic to subscribe to.
     * @param eventType    The type that event get transformed into.
     * @param eventHandler The event handler.
     */
    public void subscribe(String topicUri, Class<?> eventType, EventHandler eventHandler);

    /**
     * Subscribe to a topic. When already subscribed, overwrite the event handler.
     *
     * @param topicUri     The URI or CURIE of the topic to subscribe to.
     * @param eventType    The type that event get transformed into.
     * @param eventHandler The event handler.
     */
    public void subscribe(String topicUri, TypeReference<?> eventType, EventHandler eventHandler);

    /**
     * Unsubscribe from given topic.
     *
     * @param topicUri The URI or CURIE of the topic to unsubscribe from.
     */
    public void unsubscribe(String topicUri);

    /**
     * Unsubscribe from any topics subscribed.
     */
    public void unsubscribe();

    /**
     * Publish an event to the specified topic.
     *
     * @param topicUri The URI or CURIE of the topic the event is to be published for.
     * @param event    The event to be published.
     */
    public void publish(String topicUri, Object event);

}
