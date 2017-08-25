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

package io.crossbar.autobahn.wamp;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IInvocationHandler;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.interfaces.TriConsumer;
import io.crossbar.autobahn.wamp.interfaces.TriFunction;
import io.crossbar.autobahn.wamp.messages.Abort;
import io.crossbar.autobahn.wamp.messages.Call;
import io.crossbar.autobahn.wamp.messages.Error;
import io.crossbar.autobahn.wamp.messages.Event;
import io.crossbar.autobahn.wamp.messages.Goodbye;
import io.crossbar.autobahn.wamp.messages.Hello;
import io.crossbar.autobahn.wamp.messages.Invocation;
import io.crossbar.autobahn.wamp.messages.Publish;
import io.crossbar.autobahn.wamp.messages.Published;
import io.crossbar.autobahn.wamp.messages.Register;
import io.crossbar.autobahn.wamp.messages.Registered;
import io.crossbar.autobahn.wamp.messages.Result;
import io.crossbar.autobahn.wamp.messages.Subscribe;
import io.crossbar.autobahn.wamp.messages.Subscribed;
import io.crossbar.autobahn.wamp.messages.Welcome;
import io.crossbar.autobahn.wamp.messages.Yield;
import io.crossbar.autobahn.wamp.requests.CallRequest;
import io.crossbar.autobahn.wamp.requests.PublishRequest;
import io.crossbar.autobahn.wamp.requests.RegisterRequest;
import io.crossbar.autobahn.wamp.requests.SubscribeRequest;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.ReceptionResult;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.crossbar.autobahn.wamp.utils.IDGenerator;

import static io.crossbar.autobahn.wamp.messages.MessageMap.MESSAGE_TYPE_MAP;


public class Session implements ISession, ITransportHandler {

    private static final Logger LOGGER = Logger.getLogger(Session.class.getName());

    private final int STATE_DISCONNECTED = 1;
    private final int STATE_HELLO_SENT = 2;
    private final int STATE_AUTHENTICATE_SENT = 3;
    private final int STATE_JOINED = 4;
    private final int STATE_READY = 5;
    private final int STATE_GOODBYE_SENT = 6;
    private final int STATE_ABORT_SENT = 7;

    private ITransport mTransport;
    private ISerializer mSerializer;
    private ExecutorService mExecutor;
    private CompletableFuture<SessionDetails> mJoinFuture;

    private final ArrayList<OnJoinListener> mOnJoinListeners;
    private final ArrayList<OnReadyListener> mOnReadyListeners;
    private final ArrayList<OnLeaveListener> mOnLeaveListeners;
    private final ArrayList<OnConnectListener> mOnConnectListeners;
    private final ArrayList<OnDisconnectListener> mOnDisconnectListeners;
    private final ArrayList<OnUserErrorListener> mOnUserErrorListeners;
    private final IDGenerator mIDGenerator;
    private final Map<Long, CallRequest> mCallRequests;
    private final Map<Long, SubscribeRequest> mSubscribeRequests;
    private final Map<Long, PublishRequest> mPublishRequests;
    private final Map<Long, RegisterRequest> mRegisterRequest;
    private final Map<Long, List<Subscription>> mSubscriptions;
    private final Map<Long, Registration> mRegistrations;

    private int mState = STATE_DISCONNECTED;
    private long mSessionID;
    private boolean mGoodbyeSent;
    private String mRealm;

    public Session() {
        mOnJoinListeners = new ArrayList<>();
        mOnReadyListeners = new ArrayList<>();
        mOnLeaveListeners = new ArrayList<>();
        mOnConnectListeners = new ArrayList<>();
        mOnDisconnectListeners = new ArrayList<>();
        mOnUserErrorListeners = new ArrayList<>();
        mIDGenerator = new IDGenerator();
        mCallRequests = new HashMap<>();
        mSubscribeRequests = new HashMap<>();
        mPublishRequests = new HashMap<>();
        mRegisterRequest = new HashMap<>();
        mSubscriptions = new HashMap<>();
        mRegistrations = new HashMap<>();
    }

    public Session(ExecutorService executor) {
        this();
        mExecutor = executor;
    }

    /**
     * Returns the ID of the current session, 0 otherwise.
     * @return The session ID
     */
    public long getID() {
        return mSessionID;
    }

    private ExecutorService getExecutor() {
        return mExecutor == null ? ForkJoinPool.commonPool() : mExecutor;
    }

    private void throwIfNotConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
    }

    @Override
    public void onConnect(ITransport transport, ISerializer serializer) throws Exception {
        LOGGER.info("onConnect()");
        if (mTransport != null) {
            throw new Exception("already connected");
        }
        mTransport = transport;
        mSerializer = serializer;

        // FIXME: should be async.
        mOnConnectListeners.forEach(onConnectListener -> onConnectListener.onConnect(this));
    }

    private void send(IMessage message) {
        if (!isConnected()) {
            throw new IllegalStateException("no transport");
        }
        byte[] payload = mSerializer.serialize(message.marshal());

        LOGGER.info("  >>> TX : " + message);
        mTransport.send(payload, mSerializer.isBinary());
    }

    @Override
    public void onMessage(byte[] payload, boolean isBinary) throws Exception {
        // transform bytes to raw message:
        List<Object> rawMessage = mSerializer.unserialize(payload, isBinary);

        // transform raw message to typed message:
        try {
            int messageType = (int) rawMessage.get(0);
            Class<? extends IMessage> messageKlass = MESSAGE_TYPE_MAP.get(messageType);
            IMessage message = (IMessage) messageKlass.getMethod(
                    "parse", List.class).invoke(null, rawMessage);
            onMessage(message);
        } catch (Exception e) {
            LOGGER.info("mapping received message bytes to IMessage failed: " + e.getMessage());
        }
    }

    private void onMessage(IMessage message) throws Exception {
        LOGGER.info("  <<< RX : " + message);

        if (mSessionID == 0) {
            if (message instanceof Welcome) {
                mState = STATE_JOINED;
                Welcome msg = (Welcome) message;
                mSessionID = msg.session;
                SessionDetails details = new SessionDetails(msg.realm, msg.session);
                mJoinFuture.complete(details);
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnJoinListeners.forEach(
                        listener -> futures.add(
                                CompletableFuture.runAsync(() -> listener.onJoin(this, details),
                                        getExecutor())));
                CompletableFuture d = combineFutures(futures);
                d.thenRunAsync(() -> {
                    mState = STATE_READY;
                    mOnReadyListeners.forEach(listener -> listener.onReady(this));
                }, getExecutor());
            } else if (message instanceof Abort) {
                Abort abortMessage = (Abort) message;
                CloseDetails details = new CloseDetails(abortMessage.reason, abortMessage.message);
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnLeaveListeners.forEach(
                        l -> futures.add(
                                CompletableFuture.runAsync(() -> l.onLeave(this, details), getExecutor())));
                CompletableFuture d = combineFutures(futures);
                d.thenRun(() -> {
                    LOGGER.info("Notified Session.onLeave listeners, now closing transport");
                    mState = STATE_DISCONNECTED;
                    if (mTransport != null && mTransport.isOpen()) {
                        try {
                            mTransport.close();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }
                });
            } else {
                // FIXME: handle Challenge message here.
                LOGGER.info("FIXME (no session): unprocessed message:");
                LOGGER.info(message.toString());
            }
        } else {
            // Now that we have an active session handle all incoming messages here.
            if (message instanceof Result) {
                Result msg = (Result) message;
                CallRequest request = mCallRequests.getOrDefault(msg.request, null);
                if (request != null) {
                    mCallRequests.remove(msg.request);

                    if (request.resultType != null) {
                        // FIXME: check args length > 1 and == 0, and kwargs != null
                        // we cannot currently POJO automap these cases!

                        request.onReply.complete(mSerializer.convertValue(msg.args.get(0), request.resultType));

                    } else {
                        request.onReply.complete(new CallResult(msg.args, msg.kwargs));
                    }

                } else {
                    throw new ProtocolError(String.format(
                            "RESULT received for non-pending request ID %s", msg.request));
                }
            } else if (message instanceof Subscribed) {
                Subscribed msg = (Subscribed) message;
                SubscribeRequest request = mSubscribeRequests.getOrDefault(msg.request, null);
                if (request != null) {
                    mSubscribeRequests.remove(msg.request);
                    if (!mSubscriptions.containsKey(msg.subscription)) {
                        mSubscriptions.put(msg.subscription, new ArrayList<>());
                    }
                    Subscription subscription = new Subscription(msg.subscription, request.topic, request.handler);
                    mSubscriptions.get(msg.subscription).add(subscription);
                    request.onReply.complete(subscription);
                } else {
                    throw new ProtocolError(String.format(
                            "SUBSCRIBED received for non-pending request ID %s", msg.request));
                }
            } else if (message instanceof Event) {
                Event msg = (Event) message;
                List<Subscription> subscriptions = mSubscriptions.getOrDefault(msg.subscription, null);
                if (subscriptions == null) {
                    throw new ProtocolError(String.format(
                            "EVENT received for non-subscribed subscription ID %s", msg.subscription));
                }

                List<CompletableFuture<?>> futures = new ArrayList<>();

                subscriptions.forEach(subscription -> {
                            EventDetails details = new EventDetails(
                                    subscription, subscription.topic, -1, null, null, this);

                            CompletableFuture future = null;
                            if (subscription.handler instanceof Consumer) {
                                Consumer handler = (Consumer) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.accept(msg.args.get(0)), getExecutor());
                            } else if (subscription.handler instanceof Function) {
                                Function handler = (Function) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.apply(msg.args.get(0)), getExecutor());
                            } else if (subscription.handler instanceof BiConsumer) {
                                BiConsumer handler = (BiConsumer) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.accept(msg.args.get(0), details), getExecutor());
                            } else if (subscription.handler instanceof BiFunction) {
                                BiFunction handler = (BiFunction) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.apply(msg.args.get(0), details), getExecutor());
                            } else if (subscription.handler instanceof TriConsumer) {
                                TriConsumer handler = (TriConsumer) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.accept(msg.args, msg.kwargs, details), getExecutor());
                            } else if (subscription.handler instanceof TriFunction) {
                                TriFunction handler = (TriFunction) subscription.handler;
                                future = CompletableFuture.runAsync(
                                        () -> handler.apply(msg.args, msg.kwargs, details), getExecutor());
                            } else {
                                // FIXME: never going to reach here, though would be better to throw here.
//                                IEventHandler handler = (IEventHandler) subscription.handler;
//                                future = CompletableFuture.runAsync(
//                                        () -> handler.accept(msg.args, msg.kwargs, details), getExecutor());
                            }
                            futures.add(future);
                        }
                );

                // Not really doing anything with the combined futures.
                combineFutures(futures);
            } else if (message instanceof Published) {
                Published msg = (Published) message;
                PublishRequest request = mPublishRequests.getOrDefault(msg.request, null);
                if (request != null) {
                    mPublishRequests.remove(msg.request);
                    Publication publication = new Publication(msg.publication);
                    request.onReply.complete(publication);
                } else {
                    throw new ProtocolError(String.format(
                            "PUBLISHED received for non-pending request ID %s", msg.request));
                }
            } else if (message instanceof Registered) {
                Registered msg = (Registered) message;
                RegisterRequest request = mRegisterRequest.getOrDefault(msg.request, null);
                if (request != null) {
                    mRegisterRequest.remove(msg.request);
                    Registration registration = new Registration(
                            msg.registration, request.procedure, request.endpoint);
                    mRegistrations.put(msg.registration, registration);
                    request.onReply.complete(registration);
                } else {
                    throw new ProtocolError(String.format(
                            "REGISTERED received for already existing registration ID %s", msg.request));
                }
            } else if (message instanceof Invocation) {
                Invocation msg = (Invocation) message;
                Registration registration = mRegistrations.getOrDefault(msg.registration, null);

                if (registration != null) {

                    InvocationDetails details = new InvocationDetails(
                            registration, registration.procedure, -1, null, null, this);

                    CompletableFuture<InvocationResult> result;
                    if (registration.endpoint instanceof Supplier) {
                        Supplier endpoint = (Supplier) registration.endpoint;
                        result = (CompletableFuture<InvocationResult>) endpoint.get();
                    } else if (registration.endpoint instanceof Function) {
                        Function endpoint = (Function) registration.endpoint;
                        result = (CompletableFuture<InvocationResult>) endpoint.apply(msg.args);
                    } else if (registration.endpoint instanceof BiFunction) {
                        BiFunction endpoint = (BiFunction) registration.endpoint;
                        result = (CompletableFuture<InvocationResult>) endpoint.apply(msg.args, details);
                    } else if (registration.endpoint instanceof TriFunction) {
                        TriFunction endpoint = (TriFunction) registration.endpoint;
                        result = (CompletableFuture<InvocationResult>) endpoint.apply(msg.args, msg.kwargs, details);
                    } else {
                        IInvocationHandler endpoint = (IInvocationHandler) registration.endpoint;
                        result = endpoint.apply(msg.args, msg.kwargs, details);
                    }

                    result.whenCompleteAsync((invocationResult, invocationException) -> {
                        if (invocationException != null) {
                            LOGGER.info("FIXME: send call error: " + invocationException.getMessage());
                        }
                        else {
                            send(new Yield(msg.request, invocationResult.results, invocationResult.kwresults));
                        }
                    }, getExecutor());
                } else {
                    throw new ProtocolError(String.format(
                            "INVOCATION received for non-registered registration ID %s", msg.registration));
                }
            } else if (message instanceof Goodbye) {
                Goodbye goodbyeMessage = (Goodbye) message;
                CloseDetails details = new CloseDetails(goodbyeMessage.reason, goodbyeMessage.message);
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnLeaveListeners.forEach(
                        l -> futures.add(
                                CompletableFuture.runAsync(() -> l.onLeave(this, details), getExecutor())));
                CompletableFuture d = combineFutures(futures);
                d.thenRun(() -> {
                    LOGGER.info("Notified Session.onLeave listeners, now closing transport");
                    if (mTransport != null && mTransport.isOpen()) {
                        try {
                            mTransport.close();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }
                    mState = STATE_DISCONNECTED;
                });
            } else if (message instanceof Error) {
                Error msg = (Error) message;
                CompletableFuture<?> onReply = null;
                if (msg.requestType == Call.MESSAGE_TYPE && mCallRequests.containsKey(msg.request)) {
                    onReply = mCallRequests.get(msg.request).onReply;
                    mCallRequests.remove(msg.request);
                } else if (msg.requestType == Publish.MESSAGE_TYPE && mPublishRequests.containsKey(msg.request)) {
                    onReply = mPublishRequests.get(msg.request).onReply;
                    mPublishRequests.remove(msg.request);
                } else if (msg.requestType == Subscribe.MESSAGE_TYPE && mSubscribeRequests.containsKey(msg.request)) {
                    onReply = mSubscribeRequests.get(msg.request).onReply;
                    mSubscribeRequests.remove(msg.request);
                } else if (msg.requestType == Register.MESSAGE_TYPE && mRegisterRequest.containsKey(msg.request)) {
                    onReply = mRegisterRequest.get(msg.request).onReply;
                    mRegisterRequest.remove(msg.request);
                }
                if (onReply != null) {
                    onReply.completeExceptionally(new ApplicationError(msg.error));
                } else {
                    throw new ProtocolError(String.format(
                            "ERROR received for non-pending request_type: %s and request ID %s",
                            msg.requestType, msg.request));
                }
            } else {
                throw new ProtocolError(String.format("Unexpected message %s", message.getClass().getName()));
            }
        }
    }

    @Override
    public void onDisconnect(boolean wasClean) {
        LOGGER.info("onDisconnect(), wasClean=" + wasClean);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        mOnDisconnectListeners.forEach(
                l -> futures.add(
                        CompletableFuture.runAsync(() -> l.onDisconnect(this, wasClean), getExecutor())));
        CompletableFuture d = combineFutures(futures);
        d.thenRun(() -> {
            LOGGER.info("Notified all Session.onDisconnect listeners.");
            mTransport = null;
            mSerializer = null;
            mState = STATE_DISCONNECTED;
        });
    }

    @Override
    public boolean isConnected() {
        return mTransport != null;
    }

    private CompletableFuture combineFutures(List<CompletableFuture<?>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    private CompletableFuture<Subscription> reallySubscribe(String topic, Object handler,
                                                            SubscribeOptions options) {
        throwIfNotConnected();
        CompletableFuture<Subscription> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mSubscribeRequests.put(requestID, new SubscribeRequest(requestID, topic, future, handler));
        send(new Subscribe(requestID, options, topic));
        return future;
    }

//    @Override
//    public CompletableFuture<Subscription> subscribe(String topic,
//                                                     IEventHandler handler,
//                                                     SubscribeOptions options) {
//        return reallySubscribe(topic, handler, options);
//    }

    /**
     * Subscribes to a WAMP topic
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T> CompletableFuture<Subscription> subscribe(String topic,
                                                         Consumer<T> handler,
                                                         SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    /**
     * Subscribes to a WAMP topic. This is a convenience method that takes
     * a callback method with simplified signature
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    /**
     * Subscribes to a WAMP topic. This is a convenience method that takes
     * a callback method with simplified signature that does not return
     * anything
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T> CompletableFuture<Subscription> subscribe(String topic,
                                                         BiConsumer<T, EventDetails> handler,
                                                         SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    /**
     * Subscribes to a WAMP topic. This is a convenience method that takes
     * a callback method with simplified signature
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    /**
     * Subscribes to a WAMP topic. This is a convenience method that takes
     * a callback method with simplified signature
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T, U> CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<T, U, EventDetails> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    /**
     * Subscribes to a WAMP topic. This is a convenience method that takes
     * a callback method with simplified signature
     * @param topic URI of the topic to subscribe
     * @param handler callback method for results of publication to the topic.
     * @param options options for the subscribe
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Subscription}
     */
    @Override
    public <T, U> CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<T, U, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options);
    }

    private CompletableFuture<Publication> reallyPublish(String topic, List<Object> args,
                                                         Map<String, Object> kwargs,
                                                         PublishOptions options) {
        throwIfNotConnected();
        CompletableFuture<Publication> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mPublishRequests.put(requestID, new PublishRequest(requestID, future));
        if (options != null) {
            send(new Publish(requestID, topic, args, kwargs, options.acknowledge, options.excludeMe));
        } else {
            send(new Publish(requestID, topic, args, kwargs, true, true));
        }
        return future;
    }

    /**
     * Publishes to a previously registered WAMP topic
     * @param topic URI of the topic
     * @param args positional arguments for the topic
     * @param kwargs keyword arguments for the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                  PublishOptions options) {
        return reallyPublish(topic, args, kwargs, options);
    }

    /**
     * Publishes to a previously registered WAMP topic that takes a single
     * argument.
     * @param topic URI of the topic
     * @param arg positional argument for the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic, Object arg, PublishOptions options) {
        List<Object> args = new ArrayList<>();
        args.add(arg);
        return reallyPublish(topic, args, null, options);
    }

    /**
     * Publishes to a previously registered WAMP topic that takes multiple
     * positional arguments.
     * @param topic URI of the topic
     * @param options options for the publication
     * @param args positional arguments for the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic, PublishOptions options, Object... args) {
        return reallyPublish(topic, Arrays.asList(args), null, options);
    }

    /**
     * Publishes to a previously registered WAMP topic that takes multiple
     * positional arguments.
     * @param topic URI of the topic
     * @param args positional arguments for the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic, Object... args) {
        return reallyPublish(topic, Arrays.asList(args), null, null);
    }

    /**
     * Publishes to a previously registered WAMP topic that takes no arguments.
     * @param topic URI of the topic
     * @param options options for the publication
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic, PublishOptions options) {
        return reallyPublish(topic, null, null, options);
    }

    /**
     * Publishes to a previously registered WAMP topic that takes no arguments.
     * @param topic URI of the topic
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Publication}
     */
    @Override
    public CompletableFuture<Publication> publish(String topic) {
        return reallyPublish(topic, null, null, null);
    }

    private CompletableFuture<Registration> reallyRegister(String procedure, Object endpoint,
                                                           RegisterOptions options) {
        throwIfNotConnected();
        CompletableFuture<Registration> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mRegisterRequest.put(requestID, new RegisterRequest(requestID, future, procedure, endpoint));
        if (options != null) {
            send(new Register(requestID, procedure, options.match, options.invoke));
        } else {
            send(new Register(requestID, procedure, null, null));
        }
        return future;
    }

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    @Override
    public CompletableFuture<Registration> register(String procedure, Supplier endpoint, RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    @Override
    public CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint,
                                                    RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    @Override
    public <T> CompletableFuture<Registration> register(String procedure,
                                                        Function<T, CompletableFuture<InvocationResult>> endpoint,
                                                        RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    @Override
    public <T> CompletableFuture<Registration> register(String procedure,
                                                        BiFunction<T, InvocationDetails,
                                                                CompletableFuture<InvocationResult>> endpoint,
                                                        RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    /**
     * Registers a WAMP procedure.
     * @param procedure name of the procedure
     * @param endpoint the callee for the remote procedure
     * @param options options for the procedure registration
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.Registration}
     */
    @Override
    public <T, U> CompletableFuture<Registration> register(String procedure,
                                                           TriFunction<T, U, InvocationDetails,
                                                                   CompletableFuture<InvocationResult>> endpoint,
                                                           RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    private <T> CompletableFuture<T> reallyCall(String procedure, List<Object> args, Map<String, Object> kwargs,
                                                TypeReference<T> resultType, CallOptions options) {
        throwIfNotConnected();

        CompletableFuture<T> future = new CompletableFuture<>();

        long requestID = mIDGenerator.next();

        mCallRequests.put(requestID, new CallRequest(requestID, procedure, future, options, resultType));

        if (options == null) {
            send(new Call(requestID, procedure, args, kwargs, 0));
        } else {
            send(new Call(requestID, procedure, args, kwargs, options.timeout));
        }
        return future;
    }

    /**
     * Call a remote procedure.
     * @param procedure URI of the procedure to call
     * @param args positional arguments for the procedure
     * @param kwargs keyword arguments for the procedure
     * @param options options for the WAMP call
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.CallResult}
     */
    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        return reallyCall(procedure, args, kwargs, null, options);
    }

    /**
     * Call a remote procedure where the result needs to be resolved to a
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
    @Override
    public <T> CompletableFuture<T> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                         TypeReference<T> resultType, CallOptions options) {
        return reallyCall(procedure, args, kwargs, resultType, options);
    }

    /**
     * Call a remote procedure where the result needs to be resolved to a
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
    @Override
    public <T> CompletableFuture<T> call(String procedure, TypeReference<T> resultType, CallOptions options,
                                         Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, resultType, options);
    }

    /**
     * Join a realm on the WAMP router
     * @param realm name of the realm to join
     * @param authMethods list of authentication methods to try
     * @return a CompletableFuture that resolves to an instance of
     * {@link io.crossbar.autobahn.wamp.types.SessionDetails}
     */
    @Override
    public CompletableFuture<SessionDetails> join(String realm, List<String> authMethods) {
        LOGGER.info("Called join() with realm=" + realm);
        mRealm = realm;
        mGoodbyeSent = false;
        Map<String, Map> roles = new HashMap<>();
        roles.put("publisher", new HashMap<>());
        roles.put("subscriber", new HashMap<>());
        roles.put("caller", new HashMap<>());
        roles.put("callee", new HashMap<>());
        send(new Hello(realm, roles));
        mJoinFuture = new CompletableFuture<>();
        mState = STATE_HELLO_SENT;
        return mJoinFuture;
    }

    /**
     * Leave the currently joined WAMP session.
     * @param reason URI representing the reason to leave
     * @param message the leave message
     */
    @Override
    public void leave(String reason, String message) {
        LOGGER.info(String.format("reason=%s message=%s", reason, message));
        send(new Goodbye(reason, message));
        mState = STATE_GOODBYE_SENT;
    }

    public OnJoinListener addOnJoinListener(OnJoinListener listener) {
        return addListener(mOnJoinListeners, listener);
    }

    public void removeOnJoinListener(OnJoinListener listener) {
        removeListener(mOnJoinListeners, listener);
    }

    public OnReadyListener adOnReadyListener(OnReadyListener listener) {
        return addListener(mOnReadyListeners, listener);
    }

    public void removeOnReadyListener(OnReadyListener listener) {
        removeListener(mOnReadyListeners, listener);
    }

    public OnLeaveListener addOnLeaveListener(OnLeaveListener listener) {
        return addListener(mOnLeaveListeners, listener);
    }

    public void removeOnLeaveListener(OnLeaveListener listener) {
        removeListener(mOnLeaveListeners, listener);
    }

    public OnConnectListener addOnConnectListener(OnConnectListener listener) {
        return addListener(mOnConnectListeners, listener);
    }

    public void removeOnConnectListener(OnConnectListener listener) {
        removeListener(mOnConnectListeners, listener);
    }

    public OnDisconnectListener addOnDisconnectListener(OnDisconnectListener listener) {
        return addListener(mOnDisconnectListeners, listener);
    }

    public void removeOnDisconnectListener(OnDisconnectListener listener) {
        removeListener(mOnDisconnectListeners, listener);
    }

    public OnUserErrorListener addOnUserErrorListener(OnUserErrorListener listener) {
        return addListener(mOnUserErrorListeners, listener);
    }

    public void removeOnUserErrorListener(OnUserErrorListener listener) {
        removeListener(mOnUserErrorListeners, listener);
    }

    private <T> T addListener(ArrayList<T> listeners, T listener) {
        listeners.add(listener);
        return listener;
    }

    private <T> void removeListener(ArrayList<T> listeners, T listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
}
