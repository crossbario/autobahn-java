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

package io.crossbar.autobahn.wamp.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.ReceptionResult;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;


public interface ISession {

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<List<Object>> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<List<Object>> handler,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            Class<T> resultType);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            Class<T> resultType,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            Class<T> resultType);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            Class<T> resultType,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param resultType TypeReference encapsulating the class of the first
     *                   parameter of the callback method
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler,
            SubscribeOptions options);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails,
                    CompletableFuture<ReceptionResult>> handler);

    /**
     * Subscribes to a WAMP topic.
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails,
                    CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    CompletableFuture<Integer> unsubscribe(Subscription subscription);

    /**
     * Publishes to a WAMP topic.
     * @param topic URI of the topic
     * @param args positional arguments for the topic
     * @param kwargs keyword arguments for the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(
            String topic,
            List<Object> args,
            Map<String, Object> kwargs,
            PublishOptions options);

    /**
     * Publishes to a WAMP topic.
     * @param topic URI of the topic
     * @param arg Positional argument for the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(String topic, Object arg, PublishOptions options);

    /**
     * Publishes to a WAMP topic.
     * @param topic URI of the topic
     * @param options options for the publication
     * @param args positional arguments for the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(
            String topic,
            PublishOptions options,
            Object... args);

    /**
     * Publishes to WAMP topic.
     * @param topic URI of the topic
     * @param args positional arguments for the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(String topic, Object... args);

    /**
     * Publishes to a WAMP topic.
     * @param topic URI of the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(String topic, PublishOptions options);

    /**
     * Publishes to a WAMP topic
     * @param topic URI of the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    CompletableFuture<Publication> publish(String topic);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T> CompletableFuture<Registration> register(
            String procedure,
            Supplier<T> endpoint);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T> CompletableFuture<Registration> register(
            String procedure,
            Supplier<T> endpoint,
            RegisterOptions options);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    CompletableFuture<Registration> register(
            String procedure,
            IInvocationHandler endpoint);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    CompletableFuture<Registration> register(
            String procedure,
            IInvocationHandler endpoint,
            RegisterOptions options);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint,
            RegisterOptions options);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint,
            RegisterOptions options);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint);

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint,
            RegisterOptions options);

    CompletableFuture<Integer> unregister(Registration registration);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @param args positional arguments for the procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure, Object... args);

    <T> CompletableFuture<T> call(String procedure, TypeReference<T> resultType);

    <T> CompletableFuture<T> call(String procedure, Class<T> resultType);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @param options options for the WAMP call
     * @param args positional arguments for the procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure, CallOptions options, Object... args);

    <T> CompletableFuture<T> call(
            String procedure,
            TypeReference<T> resultType,
            CallOptions options);

    <T> CompletableFuture<T> call(String procedure, Class<T> resultType, CallOptions options);

    <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            TypeReference<T> resultType);

    <T> CompletableFuture<T> call(String procedure, List<Object> args, Class<T> resultType);

    <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            TypeReference<T> resultType,
            CallOptions options);

    <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Class<T> resultType,
            CallOptions options);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @param kwargs keyword arguments for the procedure
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure, Map<String, Object> kwargs);

    <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            TypeReference<T> resultType);

    <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            Class<T> resultType);

    <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            TypeReference<T> resultType,
            CallOptions options);

    <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            Class<T> resultType,
            CallOptions options);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @param kwargs keyword arguments for the procedure
     * @param options options for the WAMP call
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    /**
     * Calls a remote procedure.
     * @param procedure URI of the procedure to call
     * @param args positional arguments for the procedure
     * @param kwargs keyword arguments for the procedure
     * @param options options for the WAMP call
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    <T> CompletableFuture<T> call(String procedure,
                                  List<Object> args,
                                  Map<String, Object> kwargs,
                                  TypeReference<T> resultType);

    <T> CompletableFuture<T> call(String procedure,
                                  List<Object> args,
                                  Map<String, Object> kwargs,
                                  Class<T> resultType);

    /**
     * Calls a remote procedure where the result needs to be resolved to a
     * POJO.
     * @param procedure URI of the procedure to call
     * @param args positional arguments for the procedure
     * @param kwargs keyword arguments for the procedure
     * @param resultType TypeReference encapsulating the class that the
     *                   returned CompletableFuture should resolve to
     * @param options options for the WAMP call
     * @return a CompletableFuture that resolves to an instance of
     * the class provided with resultType
     */
    <T> CompletableFuture<T> call(String procedure,
                                  List<Object> args,
                                  Map<String, Object> kwargs,
                                  TypeReference<T> resultType,
                                  CallOptions options);

    <T> CompletableFuture<T> call(String procedure,
                                  List<Object> args,
                                  Map<String, Object> kwargs,
                                  Class<T> resultType,
                                  CallOptions options);

    /**
     * Calls a remote procedure where the result needs to be resolved to a
     * POJO. This is a convenience method to pass positional arguments
     * directly to the method call.
     * @param procedure URI of the procedure to call
     * @param resultType TypeReference encapsulating the class that the
     *                   returned CompletableFuture should resolve to
     * @param options options for the WAMP call
     * @param args positional arguments for the procedure
     * @return a CompletableFuture that resolves to an instance of
     * the class provided with resultType
     */
    <T> CompletableFuture<T> call(String procedure,
                                  TypeReference<T> resultType,
                                  CallOptions options,
                                  Object... args);

    <T> CompletableFuture<T> call(String procedure,
                                  TypeReference<T> resultType,
                                  Object... args);

    /**
     * Joins a realm on the WAMP router
     * @param realm name of the realm to join
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.SessionDetails}
     */
    CompletableFuture<SessionDetails> join(String realm);

    /**
     * Joins a realm on the WAMP router
     * @param realm name of the realm to join
     * @param authenticators list of authentication methods to try
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.SessionDetails}
     */
    CompletableFuture<SessionDetails> join(String realm, List<IAuthenticator> authenticators);

    /**
     * Leaves the currently joined WAMP session.
     */
    void leave();

    /**
     * Leaves the currently joined WAMP session.
     * @param reason URI representing the reason to leave
     */
    void leave(String reason);

    /**
     * Leaves the currently joined WAMP session.
     * @param reason URI representing the reason to leave
     * @param message the leave message
     */
    void leave(String reason, String message);

    boolean isConnected();

    interface OnJoinListener {
        void onJoin(Session session, SessionDetails details);
    }

    interface OnReadyListener {
        void onReady(Session session);
    }

    interface OnLeaveListener {
        void onLeave(Session session, CloseDetails details);
    }

    interface OnConnectListener {
        void onConnect(Session session);
    }

    interface OnDisconnectListener {
        void onDisconnect(Session session, boolean wasClean);
    }

    // FIXME: come up with an equivalent of txaio.IFailedFuture as first arg.
    interface OnUserErrorListener {
        void onUserError(Session session, String message);
    }
}
