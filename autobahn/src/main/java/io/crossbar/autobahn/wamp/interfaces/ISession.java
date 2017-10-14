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

    CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<List<Object>> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<List<Object>> handler,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType);

    <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options);

    CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler,
            SubscribeOptions options);

    CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails, CompletableFuture<ReceptionResult>> handler);

    CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options);

    CompletableFuture<Publication> publish(
            String topic,
            List<Object> args,
            Map<String, Object> kwargs,
            PublishOptions options);

    CompletableFuture<Publication> publish(String topic, Object object, PublishOptions options);

    CompletableFuture<Publication> publish(
            String topic,
            PublishOptions options,
            Object... objects);

    CompletableFuture<Publication> publish(String topic, Object... objects);

    CompletableFuture<Publication> publish(String topic, PublishOptions options);

    CompletableFuture<Publication> publish(String topic);

    <T> CompletableFuture<Registration> register(
            String procedure,
            Supplier<T> endpoint);

    <T> CompletableFuture<Registration> register(
            String procedure,
            Supplier<T> endpoint,
            RegisterOptions options);

    CompletableFuture<Registration> register(
            String procedure,
            IInvocationHandler endpoint);

    CompletableFuture<Registration> register(
            String procedure,
            IInvocationHandler endpoint,
            RegisterOptions options);

    <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint);

    <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint,
            RegisterOptions options);

    <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint);

    <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint,
            RegisterOptions options);

    <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint);

    <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint,
            RegisterOptions options);

    CompletableFuture<CallResult> call(String procedure);

    CompletableFuture<CallResult> call(String procedure, Object... args);

    CompletableFuture<CallResult> call(String procedure, CallOptions options, Object... args);

    CompletableFuture<CallResult> call(String procedure, Map<String, Object> kwargs);

    CompletableFuture<CallResult> call(String procedure,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    <T> CompletableFuture<T> call(String procedure,
                                  List<Object> args,
                                  Map<String, Object> kwargs,
                                  TypeReference<T> resultType,
                                  CallOptions options);

    <T> CompletableFuture<T> call(String procedure,
                                  TypeReference<T> resultType,
                                  CallOptions options,
                                  Object... args);

    CompletableFuture<SessionDetails> join(String realm);

    CompletableFuture<SessionDetails> join(String realm, List<String> authMethods);

    void leave();

    void leave(String reason);

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
