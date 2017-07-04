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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IEventHandler;
import io.crossbar.autobahn.wamp.interfaces.IInvocationHandler;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
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
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.crossbar.autobahn.wamp.utils.IDGenerator;

import static io.crossbar.autobahn.wamp.messages.MessageMap.MESSAGE_TYPE_MAP;


public class Session implements ISession, ITransportHandler {

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

    public long getID() {
        return mSessionID;
    }

    private ExecutorService getExecutor() {
        if (mExecutor == null) {
            mExecutor = ForkJoinPool.commonPool();
        }
        return mExecutor;
    }

    @Override
    public void onConnect(ITransport transport, ISerializer serializer) {
        System.out.println("Session.onConnect");
        if (mTransport != null) {
            // Now allowed to throw here, find a better way.
//            throw new Exception("already connected");
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

        System.out.println("  >>> TX : " + message);
        mTransport.send(payload, true);
    }

    @Override
    public void onMessage(byte[] payload, boolean isBinary) {
        // transform bytes to raw message:
        List<Object> rawMessage = mSerializer.unserialize(payload, isBinary);

        // transform raw message to typed message:
        IMessage message = null;
        try {
            int messageType = (int) rawMessage.get(0);
            Class<? extends IMessage> messageKlass = MESSAGE_TYPE_MAP.get(messageType);
            message = (IMessage) messageKlass.getMethod("parse", List.class).invoke(null, rawMessage);

        } catch (Exception e) {
            System.out.println("mapping received message bytes to IMessage failed: " + e.getMessage());
        }

        if (message != null) {
            this.onMessage(message);
        }
    }

    private void onMessage(IMessage message) {
        System.out.println("  <<< RX : " + message);

        if (mSessionID == 0) {
            if (message instanceof Welcome) {
                mState = STATE_JOINED;
                Welcome msg = (Welcome) message;
                mSessionID = msg.session;
                SessionDetails details = new SessionDetails(msg.realm, msg.session);
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
            } else {
                // with (mSessionID == 0), we can only receive
                // WELCOME, ABORT or CHALLENGE
                System.out.println("FIXME (no session): unprocessed message:");
                System.out.println(message);
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
                if (subscriptions != null) {

                    List<CompletableFuture<?>> futures = new ArrayList<>();

                    subscriptions.forEach(
                            subscription -> {
                                EventDetails details = new EventDetails(
                                        subscription, subscription.topic, -1, null, null, this);
                                futures.add(
                                    CompletableFuture.runAsync(
                                        () -> subscription.handler.run(msg.args, msg.kwargs, details)
                                        , getExecutor()
                                    )
                                );
                            }
                    );

                    // Not really doing anything with the combined futures.
                    combineFutures(futures);
                } else {
                    throw new ProtocolError(String.format(
                            "EVENT received for non-subscribed subscription ID %s", msg.subscription));
                }
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
                    CompletableFuture<InvocationResult> result = registration.endpoint.run(
                            msg.args, msg.kwargs, details);

                    result.whenCompleteAsync((invocationResult, invocationException) -> {
                        if (invocationException != null) {
                            System.out.println("FIXME: send call error: " + invocationException.getMessage());
                        }
                        else {
                            this.send(new Yield(msg.request, invocationResult.results, invocationResult.kwresults));
                        }
                    }, getExecutor());
                } else {
                    throw new ProtocolError(String.format(
                            "INVOCATION received for non-registered registration ID %s", msg.registration));
                }
            } else if (message instanceof Goodbye) {
                CloseDetails details = new CloseDetails();
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnLeaveListeners.forEach(
                        l -> futures.add(
                                CompletableFuture.runAsync(() -> l.onLeave(this, details), getExecutor())));
                CompletableFuture d = combineFutures(futures);
                d.thenRun(() -> {
                    System.out.println("CLOSED NOW");
                    mState = STATE_DISCONNECTED;
                    if (mTransport != null && mTransport.isOpen()) {
                        mTransport.close();
                    }
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
        System.out.println("Session.onDisconnect");

        List<CompletableFuture<?>> futures = new ArrayList<>();
        mOnDisconnectListeners.forEach(
                l -> futures.add(
                        CompletableFuture.runAsync(() -> l.onDisconnect(this, wasClean), getExecutor())));
        CompletableFuture d = combineFutures(futures);
        d.thenRun(() -> {
            System.out.println("DISCONNECTED NOW");
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

    @Override
    public CompletableFuture<Subscription> subscribe(String topic, IEventHandler handler, SubscribeOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
        CompletableFuture<Subscription> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mSubscribeRequests.put(requestID, new SubscribeRequest(requestID, topic, future, handler));
        this.send(new Subscribe(requestID, options, topic));
        return future;
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(String topic, Consumer<T> handler,
                                                         SubscribeOptions options) {
        return null;
    }

    @Override
    public <T, U> CompletableFuture<Subscription> subscribe(String topic, BiConsumer<T, U> handler,
                                                            SubscribeOptions options) {
        return null;
    }

    private CompletableFuture<Publication> reallyPublish(String topic, List<Object> args,
                                                         Map<String, Object> kwargs,
                                                         PublishOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
        CompletableFuture<Publication> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mPublishRequests.put(requestID, new PublishRequest(requestID, future));
        if (options != null) {
            this.send(new Publish(requestID, topic, args, kwargs, options.acknowledge, options.excludeMe));
        } else {
            this.send(new Publish(requestID, topic, args, kwargs, true, true));
        }
        return future;
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                  PublishOptions options) {
        return reallyPublish(topic, args, kwargs, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, Object object, PublishOptions options) {
        List<Object> args = new ArrayList<>();
        args.add(object);
        return reallyPublish(topic, args, null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, PublishOptions options, Object... objects) {
        return reallyPublish(topic, Arrays.asList(objects), null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, Object... objects) {
        return reallyPublish(topic, Arrays.asList(objects), null, null);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, PublishOptions options) {
        return reallyPublish(topic, null, null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic) {
        return reallyPublish(topic, null, null, null);
    }

    @Override
    public CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint,
                                                    RegisterOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
        CompletableFuture<Registration> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mRegisterRequest.put(requestID, new RegisterRequest(requestID, future, procedure, endpoint));
        if (options != null) {
            this.send(new Register(requestID, procedure, options.match, options.invoke));
        } else {
            this.send(new Register(requestID, procedure, null, null));
        }
        return future;
    }

    private <T> CompletableFuture<T> reallyCall(String procedure, List<Object> args, Map<String, Object> kwargs,
                                                TypeReference<T> resultType, CallOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }

        CompletableFuture<T> future = new CompletableFuture<>();

        long requestID = mIDGenerator.next();

        mCallRequests.put(requestID, new CallRequest(requestID, procedure, future, options, resultType));

        if (options == null) {
            this.send(new Call(requestID, procedure, args, kwargs, 0));
        } else {
            this.send(new Call(requestID, procedure, args, kwargs, options.timeout));
        }
        return future;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        return reallyCall(procedure, args, kwargs, null, options);
    }

    @Override
    public <T> CompletableFuture<T> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                         TypeReference<T> resultType, CallOptions options) {
        return reallyCall(procedure, args, kwargs, resultType, options);
    }

    @Override
    public <T> CompletableFuture<T> call(String procedure, TypeReference<T> resultType, CallOptions options,
                                         Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, resultType, options);
    }

    @Override
    public CompletableFuture<SessionDetails> join(String realm, List<String> authMethods) {
        System.out.println("Session.join");
        mRealm = realm;
        mGoodbyeSent = false;
        Map<String, Map> roles = new HashMap<>();
        roles.put("publisher", new HashMap<>());
        roles.put("subscriber", new HashMap<>());
        roles.put("caller", new HashMap<>());
        roles.put("callee", new HashMap<>());
        this.send(new Hello(realm, roles));
        mState = STATE_HELLO_SENT;
        return null;
    }

    @Override
    public void leave(String reason, String message) {
        System.out.println("Session.leave");
        this.send(new Goodbye(reason, message));
        mState = STATE_GOODBYE_SENT;
    }

    public OnJoinListener addOnJoinListener(OnJoinListener listener) {
        mOnJoinListeners.add(listener);
        return listener;
    }

    public void removeOnJoinListener(OnJoinListener listener) {
        if (mOnJoinListeners.contains(listener)) {
            mOnJoinListeners.remove(listener);
        }
    }

    public OnReadyListener adOnReadyListener(OnReadyListener listener) {
        mOnReadyListeners.add(listener);
        return listener;
    }

    public void removeOnReadyListener(OnReadyListener listener) {
        if (mOnReadyListeners.contains(listener)) {
            mOnReadyListeners.remove(listener);
        }
    }

    public OnLeaveListener addOnLeaveListener(OnLeaveListener listener) {
        mOnLeaveListeners.add(listener);
        return listener;
    }

    public void removeOnLeaveListener(OnLeaveListener listener) {
        if (mOnLeaveListeners.contains(listener)) {
            mOnLeaveListeners.remove(listener);
        }
    }

    public OnConnectListener addOnConnectListener(OnConnectListener listener) {
        mOnConnectListeners.add(listener);
        return listener;
    }

    public void removeOnConnectListener(OnConnectListener listener) {
        if (mOnConnectListeners.contains(listener)) {
            mOnConnectListeners.remove(listener);
        }
    }

    public OnDisconnectListener addOnDisconnectListener(OnDisconnectListener listener) {
        mOnDisconnectListeners.add(listener);
        return listener;
    }

    public void removeOnDisconnectListener(OnDisconnectListener listener) {
        if (mOnDisconnectListeners.contains(listener)) {
            mOnDisconnectListeners.remove(listener);
        }
    }

    public OnUserErrorListener addOnUserErrorListener(OnUserErrorListener listener) {
        mOnUserErrorListeners.add(listener);
        return listener;
    }

    public void removeOnUserErrorListener(OnUserErrorListener listener) {
        if (mOnUserErrorListeners.contains(listener)) {
            mOnUserErrorListeners.remove(listener);
        }
    }
}
