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
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.auth.ChallengeResponseAuth;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.auth.TicketAuth;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.interfaces.IInvocationHandler;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.interfaces.TriConsumer;
import io.crossbar.autobahn.wamp.interfaces.TriFunction;
import io.crossbar.autobahn.wamp.messages.Abort;
import io.crossbar.autobahn.wamp.messages.Authenticate;
import io.crossbar.autobahn.wamp.messages.Call;
import io.crossbar.autobahn.wamp.messages.Challenge;
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
import io.crossbar.autobahn.wamp.messages.Unregister;
import io.crossbar.autobahn.wamp.messages.Unregistered;
import io.crossbar.autobahn.wamp.messages.Unsubscribe;
import io.crossbar.autobahn.wamp.messages.Unsubscribed;
import io.crossbar.autobahn.wamp.messages.Welcome;
import io.crossbar.autobahn.wamp.messages.Yield;
import io.crossbar.autobahn.wamp.reflectionRoles.ReflectionServices;
import io.crossbar.autobahn.wamp.reflectionRoles.WampException;
import io.crossbar.autobahn.wamp.requests.CallRequest;
import io.crossbar.autobahn.wamp.requests.PublishRequest;
import io.crossbar.autobahn.wamp.requests.RegisterRequest;
import io.crossbar.autobahn.wamp.requests.SubscribeRequest;
import io.crossbar.autobahn.wamp.requests.UnregisterRequest;
import io.crossbar.autobahn.wamp.requests.UnsubscribeRequest;
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
import io.crossbar.autobahn.wamp.utils.Platform;

import static io.crossbar.autobahn.wamp.messages.MessageMap.MESSAGE_TYPE_MAP;
import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;
import static java.util.concurrent.CompletableFuture.runAsync;


public class Session implements ISession, ITransportHandler {

    public static final IABLogger LOGGER = ABLogger.getLogger(Session.class.getName());

    private final int STATE_DISCONNECTED = 1;
    private final int STATE_HELLO_SENT = 2;
    private final int STATE_AUTHENTICATE_SENT = 3;
    private final int STATE_JOINED = 4;
    private final int STATE_READY = 5;
    private final int STATE_GOODBYE_SENT = 6;
    private final int STATE_ABORT_SENT = 7;

    private ITransport mTransport;
    private ISerializer mSerializer;
    private Executor mExecutor;
    private CompletableFuture<SessionDetails> mJoinFuture;
    private List<IAuthenticator> mAuthenticators;

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
    private final Map<Long, UnsubscribeRequest> mUnsubscribeRequests;
    private final Map<Long, UnregisterRequest> mUnregisterRequests;

    private int mState = STATE_DISCONNECTED;
    private long mSessionID;
    private boolean mGoodbyeSent;
    private String mRealm;
    private ReflectionServices mReflectionServices;

    public Session() {
        this(Platform.autoSelectExecutor());
    }

    public Session(Executor executor) {
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
        mUnsubscribeRequests = new HashMap<>();
        mUnregisterRequests = new HashMap<>();
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        mExecutor = executor;
    }

    /**
     * Returns the ID of the current session, 0 otherwise.
     * @return The session ID
     */
    public long getID() {
        return mSessionID;
    }

    private void throwIfNotConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
    }

    @Override
    public void onConnect(ITransport transport, ISerializer serializer) throws Exception {
        LOGGER.d("onConnect()");
        if (mTransport != null) {
            throw new Exception("already connected");
        }
        mTransport = transport;
        mSerializer = serializer;
        mReflectionServices = new ReflectionServices(this, mSerializer);

        runAsync(() -> {
            for (OnConnectListener listener: mOnConnectListeners) {
                listener.onConnect(this);
            }
        }, mExecutor);
    }

    private void send(IMessage message) {
        if (!isConnected()) {
            throw new IllegalStateException("no transport");
        }

        LOGGER.d("  >>> TX : " + message);
        mTransport.send(mSerializer.serialize(message.marshal()), mSerializer.isBinary());
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
            LOGGER.d("  <<< RX : " + message);
            if (mSessionID == 0) {
                onPreSessionMessage(message);
            } else {
                onMessage(message);
            }
        } catch (Exception e) {
            LOGGER.d("mapping received message bytes to IMessage failed: " + e.getMessage());
        }
    }

    @Override
    public void onLeave(CloseDetails details) {
        if (mState == STATE_DISCONNECTED) {
            return;
        }
        LOGGER.d("onLeave(), reason=" + details.reason);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (OnLeaveListener listener: mOnLeaveListeners) {
            futures.add(runAsync(() -> listener.onLeave(this, details), mExecutor));
        }
        CompletableFuture d = combineFutures(futures);
        d.thenRunAsync(() -> {
            LOGGER.d("Notified all Session.onLeave listeners.");
        }, mExecutor);
    }

    private void onPreSessionMessage(IMessage message) throws Exception {
        if (message instanceof Welcome) {
            Welcome msg = (Welcome) message;
            mState = STATE_JOINED;
            mSessionID = msg.session;
            SessionDetails details = new SessionDetails(msg.realm, msg.session);
            mJoinFuture.complete(details);
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (OnJoinListener listener: mOnJoinListeners) {
                futures.add(runAsync(() -> listener.onJoin(this, details), mExecutor));
            }
            CompletableFuture d = combineFutures(futures);
            d.thenRunAsync(() -> {
                mState = STATE_READY;
                for (OnReadyListener listener: mOnReadyListeners) {
                    listener.onReady(this);
                }
            }, mExecutor);
        } else if (message instanceof Abort) {
            Abort abortMessage = (Abort) message;
            CloseDetails details = new CloseDetails(abortMessage.reason, abortMessage.message);
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (OnLeaveListener listener: mOnLeaveListeners) {
                futures.add(runAsync(() -> listener.onLeave(this, details), mExecutor));
            }
            CompletableFuture d = combineFutures(futures);
            d.thenRunAsync(() -> {
                LOGGER.d("Notified Session.onLeave listeners, now closing transport");
                mState = STATE_DISCONNECTED;
                if (mTransport != null && mTransport.isOpen()) {
                    try {
                        mTransport.close();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }
            }, mExecutor);
        } else if (message instanceof Challenge) {
            Challenge msg = (Challenge) message;
            io.crossbar.autobahn.wamp.types.Challenge challenge =
                    new io.crossbar.autobahn.wamp.types.Challenge(msg.method, msg.extra);
            if (mAuthenticators != null) {
                if (msg.method.equals(TicketAuth.authmethod)) {
                    for (IAuthenticator authenticator: mAuthenticators) {
                        if (authenticator.getAuthMethod().equals(TicketAuth.authmethod)) {
                            TicketAuth auth = (TicketAuth) authenticator;
                            auth.onChallenge(this, challenge).whenCompleteAsync(
                                    (response, throwable) -> send(new Authenticate(
                                            response.signature, response.extra)), mExecutor);
                            break;
                        }
                    }
                } else if (msg.method.equals(ChallengeResponseAuth.authmethod)) {
                    for (IAuthenticator authenticator: mAuthenticators) {
                        if (authenticator.getAuthMethod().equals(
                                ChallengeResponseAuth.authmethod)) {
                            ChallengeResponseAuth auth = (ChallengeResponseAuth) authenticator;
                            auth.onChallenge(this, challenge).whenCompleteAsync(
                                    (response, throwable) -> send(new Authenticate(
                                            response.signature, response.extra)), mExecutor);
                            break;
                        }
                    }
                } else if (msg.method.equals(CryptosignAuth.authmethod)) {
                    for (IAuthenticator authenticator: mAuthenticators) {
                        if (authenticator.getAuthMethod().equals(CryptosignAuth.authmethod)) {
                            CryptosignAuth auth = (CryptosignAuth) authenticator;
                            auth.onChallenge(this, challenge).whenCompleteAsync(
                                    (response, throwable) -> send(new Authenticate(
                                            response.signature, response.extra)), mExecutor);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void onMessage(IMessage message) throws Exception {
        if (message instanceof Result) {
            Result msg = (Result) message;
            CallRequest request = getOrDefault(mCallRequests, msg.request, null);
            if (request == null) {
                throw new ProtocolError(String.format(
                        "RESULT received for non-pending request ID %s", msg.request));
            }

            mCallRequests.remove(msg.request);
            if (request.resultTypeRef != null) {
                // FIXME: check args length > 1 and == 0, and kwargs != null
                // we cannot currently POJO automap these cases!
                request.onReply.complete(mSerializer.convertValue(
                        msg.args.get(0), request.resultTypeRef));
            } else if (request.resultTypeClass != null) {
                request.onReply.complete(mSerializer.convertValue(
                        msg.args.get(0), request.resultTypeClass));
            } else {
                request.onReply.complete(new CallResult(msg.args, msg.kwargs));
            }
        } else if (message instanceof Subscribed) {
            Subscribed msg = (Subscribed) message;
            SubscribeRequest request = getOrDefault(mSubscribeRequests, msg.request, null);
            if (request == null) {
                throw new ProtocolError(String.format(
                        "SUBSCRIBED received for non-pending request ID %s", msg.request));
            }

            mSubscribeRequests.remove(msg.request);
            if (!mSubscriptions.containsKey(msg.subscription)) {
                mSubscriptions.put(msg.subscription, new ArrayList<>());
            }
            Subscription subscription = new Subscription(msg.subscription, request.topic,
                    request.resultTypeRef, request.resultTypeClass, request.handler, this);
            mSubscriptions.get(msg.subscription).add(subscription);
            request.onReply.complete(subscription);
        } else if (message instanceof Event) {
            Event msg = (Event) message;
            List<Subscription> subscriptions = getOrDefault(mSubscriptions, msg.subscription, null);
            if (subscriptions == null) {
                throw new ProtocolError(String.format(
                        "EVENT received for non-subscribed subscription ID %s", msg.subscription));
            }

            List<CompletableFuture<?>> futures = new ArrayList<>();

            for (Subscription subscription: subscriptions) {
                EventDetails details = new EventDetails(
                        subscription, msg.publication,
                        msg.topic != null ? msg.topic : subscription.topic,
                        msg.retained, -1, null,
                        null, this);

                CompletableFuture future = null;
                // Check if we expect a POJO.
                Object arg;
                if (subscription.resultTypeRef != null) {
                    arg = mSerializer.convertValue(msg.args.get(0), subscription.resultTypeRef);
                } else if (subscription.resultTypeClass != null) {
                    arg = mSerializer.convertValue(msg.args.get(0), subscription.resultTypeClass);
                } else {
                    arg = msg.args;
                }

                if (subscription.handler instanceof Consumer) {
                    Consumer handler = (Consumer) subscription.handler;
                    future = runAsync(() -> handler.accept(arg), mExecutor);
                } else if (subscription.handler instanceof Function) {
                    Function handler = (Function) subscription.handler;
                    future = runAsync(() -> handler.apply(arg), mExecutor);
                } else if (subscription.handler instanceof BiConsumer) {
                    BiConsumer handler = (BiConsumer) subscription.handler;
                    future = runAsync(() -> handler.accept(arg, details), mExecutor);
                } else if (subscription.handler instanceof BiFunction) {
                    BiFunction handler = (BiFunction) subscription.handler;
                    future = runAsync(() -> handler.apply(arg, details), mExecutor);
                } else if (subscription.handler instanceof TriConsumer) {
                    TriConsumer handler = (TriConsumer) subscription.handler;
                    future = runAsync(
                            () -> handler.accept(arg, msg.kwargs, details), mExecutor);
                } else if (subscription.handler instanceof TriFunction) {
                    TriFunction handler = (TriFunction) subscription.handler;
                    future = runAsync(() -> handler.apply(arg, msg.kwargs, details), mExecutor);
                } else {
                    // FIXME: never going to reach here, though would be better to throw.
                }
                futures.add(future);
            }

            // Not really doing anything with the combined futures.
            combineFutures(futures);
        } else if (message instanceof Published) {
            Published msg = (Published) message;
            PublishRequest request = getOrDefault(mPublishRequests, msg.request, null);
            if (request == null) {
                throw new ProtocolError(String.format(
                        "PUBLISHED received for non-pending request ID %s", msg.request));
            }

            mPublishRequests.remove(msg.request);
            Publication publication = new Publication(msg.publication);
            request.onReply.complete(publication);
        } else if (message instanceof Registered) {
            Registered msg = (Registered) message;
            RegisterRequest request = getOrDefault(mRegisterRequest, msg.request, null);

            if (request == null) {
                throw new ProtocolError(String.format(
                        "REGISTERED received for already existing registration ID %s",
                        msg.request));
            }
            mRegisterRequest.remove(msg.request);
            Registration registration = new Registration(
                    msg.registration, request.procedure, request.endpoint, this);
            mRegistrations.put(msg.registration, registration);
            request.onReply.complete(registration);
        } else if (message instanceof Invocation) {
            Invocation msg = (Invocation) message;
            Registration registration = getOrDefault(mRegistrations, msg.registration, null);

            if (registration == null) {
                throw new ProtocolError(String.format(
                        "INVOCATION received for non-registered registration ID %s",
                        msg.registration));
            }

            InvocationDetails details = new InvocationDetails(
                    registration, registration.procedure, -1, null, null, this);

            runAsync(() -> {
                Object result;
                if (registration.endpoint instanceof Supplier) {
                    Supplier endpoint = (Supplier) registration.endpoint;
                    result = endpoint.get();
                } else if (registration.endpoint instanceof Function) {
                    Function endpoint = (Function) registration.endpoint;
                    result = endpoint.apply(msg.args);
                } else if (registration.endpoint instanceof BiFunction) {
                    BiFunction endpoint = (BiFunction) registration.endpoint;
                    result = endpoint.apply(msg.args, details);
                } else if (registration.endpoint instanceof TriFunction) {
                    TriFunction endpoint = (TriFunction) registration.endpoint;
                    result = endpoint.apply(msg.args, msg.kwargs, details);
                } else {
                    IInvocationHandler endpoint = (IInvocationHandler) registration.endpoint;
                    result = endpoint.apply(msg.args, msg.kwargs, details);
                }

                if (result instanceof CompletableFuture) {
                    CompletableFuture<InvocationResult> fResult =
                            (CompletableFuture<InvocationResult>) result;
                    fResult.whenCompleteAsync((invocRes, throwable) -> {
                        if (throwable != null) {

                            if (throwable instanceof WampException){
                                WampException casted = (WampException) throwable;
                                send(new Error(Invocation.MESSAGE_TYPE, msg.request,
                                        casted.getErrorUri(), casted.getArguments(), casted.getKwArguments()));
                            }
                            else{
                                List<Object> args = new ArrayList<>();
                                args.add(throwable.getMessage());
                                send(new Error(Invocation.MESSAGE_TYPE, msg.request,
                                        "wamp.error.runtime_error", args, null));
                            }

                        } else {
                            send(new Yield(msg.request, invocRes.results, invocRes.kwresults));
                        }
                    }, mExecutor);
                } else if (result instanceof InvocationResult) {
                    InvocationResult res = (InvocationResult) result;
                    send(new Yield(msg.request, res.results, res.kwresults));
                } else if (result instanceof List) {
                    send(new Yield(msg.request, (List) result, null));
                } else if (result instanceof Map) {
                    send(new Yield(msg.request, null, (Map) result));
                } else if (result instanceof Void) {
                    send(new Yield(msg.request, null, null));
                } else {
                    List<Object> item = new ArrayList<>();
                    item.add(result);
                    send(new Yield(msg.request, item, null));
                }
            }, mExecutor).whenCompleteAsync((aVoid, throwable) -> {
                // FIXME: implement better errors
                if (throwable != null) {
                    if (throwable instanceof WampException){
                        WampException casted = (WampException) throwable;
                        send(new Error(Invocation.MESSAGE_TYPE, msg.request,
                                casted.getErrorUri(), casted.getArguments(), casted.getKwArguments()));
                    }
                    else
                    {
                        List<Object> args = new ArrayList<>();
                        args.add(throwable.getMessage());
                        send(new Error(Invocation.MESSAGE_TYPE, msg.request, "wamp.error.runtime_error",
                                args, null));
                    }
                }
            });
        } else if (message instanceof Goodbye) {
            Goodbye msg = (Goodbye) message;
            CloseDetails details = new CloseDetails(msg.reason, msg.message);
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (OnLeaveListener listener: mOnLeaveListeners) {
                futures.add(runAsync(() -> listener.onLeave(this, details), mExecutor));
            }
            CompletableFuture d = combineFutures(futures);
            d.thenRunAsync(() -> {
                LOGGER.d("Notified Session.onLeave listeners, now closing transport");
                if (mTransport != null && mTransport.isOpen()) {
                    try {
                        mTransport.close();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }
                mState = STATE_DISCONNECTED;
            }, mExecutor);
        } else if (message instanceof Unregistered) {
            Unregistered msg = (Unregistered) message;
            UnregisterRequest request = getOrDefault(mUnregisterRequests, msg.request, null);
            if (request == null) {
                throw new ProtocolError(String.format(
                        "UNREGISTERED received for already unregistered registration ID %s",
                        msg.registration));
            }
            if (mRegistrations.containsKey(request.registrationID)) {
                mRegistrations.remove(request.registrationID);
            }
            request.onReply.complete(0);
        } else if (message instanceof Unsubscribed) {
            Unsubscribed msg = (Unsubscribed) message;
            UnsubscribeRequest request = getOrDefault(mUnsubscribeRequests, msg.request, null);
            List<Subscription> subscriptions = mSubscriptions.get(request.subscriptionID);
            request.onReply.complete(subscriptions.size());
        } else if (message instanceof Error) {
            Error msg = (Error) message;
            CompletableFuture<?> onReply = null;
            if (msg.requestType == Call.MESSAGE_TYPE && mCallRequests.containsKey(msg.request)) {
                onReply = mCallRequests.get(msg.request).onReply;
                mCallRequests.remove(msg.request);
            } else if (msg.requestType == Publish.MESSAGE_TYPE
                    && mPublishRequests.containsKey(msg.request)) {
                onReply = mPublishRequests.get(msg.request).onReply;
                mPublishRequests.remove(msg.request);
            } else if (msg.requestType == Subscribe.MESSAGE_TYPE
                    && mSubscribeRequests.containsKey(msg.request)) {
                onReply = mSubscribeRequests.get(msg.request).onReply;
                mSubscribeRequests.remove(msg.request);
            } else if (msg.requestType == Register.MESSAGE_TYPE
                    && mRegisterRequest.containsKey(msg.request)) {
                onReply = mRegisterRequest.get(msg.request).onReply;
                mRegisterRequest.remove(msg.request);
            }
            if (onReply != null) {
                onReply.completeExceptionally(new ApplicationError(
                        msg.error, msg.args, msg.kwargs));
            } else {
                throw new ProtocolError(String.format(
                        "ERROR received for non-pending request_type: %s and request ID %s",
                        msg.requestType, msg.request));
            }
        } else {
            throw new ProtocolError(String.format("Unexpected message %s",
                    message.getClass().getName()));
        }
    }

    @Override
    public void onDisconnect(boolean wasClean) {
        LOGGER.d("onDisconnect(), wasClean=" + wasClean);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (OnDisconnectListener listener: mOnDisconnectListeners) {
            futures.add(runAsync(() -> listener.onDisconnect(this, wasClean), mExecutor));
        }
        CompletableFuture d = combineFutures(futures);
        d.thenRunAsync(() -> {
            LOGGER.d("Notified all Session.onDisconnect listeners.");
            mTransport = null;
            mSerializer = null;
            mState = STATE_DISCONNECTED;
        }, mExecutor);
    }

    @Override
    public boolean isConnected() {
        return mTransport != null;
    }

    private CompletableFuture combineFutures(List<CompletableFuture<?>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    private <T> CompletableFuture<Subscription> reallySubscribe(
            String topic,
            Object handler,
            SubscribeOptions options,
            TypeReference<T> resultTypeRef,
            Class<T> resultTypeClass) {
        throwIfNotConnected();
        CompletableFuture<Subscription> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mSubscribeRequests.put(requestID, new SubscribeRequest(requestID, topic, future,
                resultTypeRef, resultTypeClass, handler));
        send(new Subscribe(requestID, options, topic));
        return future;
    }

    @Override
    public CompletableFuture<Subscription> subscribe(String topic, Consumer<List<Object>> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<List<Object>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType) {
        return reallySubscribe(topic, handler, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            Class<T> resultType) {
        return reallySubscribe(topic, handler, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            TypeReference<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Consumer<T> handler,
            Class<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, resultType);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            Function<List<Object>, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T,CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType) {
        return reallySubscribe(topic, handler, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType) {
        return reallySubscribe(topic, handler, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            Function<T, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, resultType);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<List<Object>, EventDetails> handler, SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType) {
        return reallySubscribe(topic, handler, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            Class<T> resultType) {
        return reallySubscribe(topic, handler, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            TypeReference<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiConsumer<T, EventDetails> handler,
            Class<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, resultType);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<List<Object>, EventDetails, CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType) {
        return reallySubscribe(topic, handler, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType) {
        return reallySubscribe(topic, handler, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            TypeReference<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<Subscription> subscribe(
            String topic,
            BiFunction<T, EventDetails, CompletableFuture<ReceptionResult>> handler,
            Class<T> resultType,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, resultType);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            TriConsumer<List<Object>, Map<String, Object>, EventDetails> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails,
                    CompletableFuture<ReceptionResult>> handler) {
        return reallySubscribe(topic, handler, null, null, null);
    }

    @Override
    public CompletableFuture<Subscription> subscribe(
            String topic,
            TriFunction<List<Object>, Map<String, Object>, EventDetails,
                    CompletableFuture<ReceptionResult>> handler,
            SubscribeOptions options) {
        return reallySubscribe(topic, handler, options, null, null);
    }

    @Override
    public CompletableFuture<Integer> unsubscribe(Subscription subscription) {
        if (!subscription.isActive()) {
            throw new IllegalStateException("Subscription is already inactive");
        }
        List<Subscription> subscriptions = getOrDefault(mSubscriptions, subscription.subscription,
                null);

        if (subscriptions == null || !subscriptions.contains(subscription)) {
            throw new IllegalStateException("Subscription is already inactive");
        }

        subscriptions.remove(subscription);
        subscription.setInactive();
        int remainingCount = subscriptions.size();
        CompletableFuture<Integer> unsubFuture = new CompletableFuture<>();
        if (remainingCount == 0) {
            long requestID = mIDGenerator.next();
            mUnsubscribeRequests.put(requestID, new UnsubscribeRequest(requestID, unsubFuture,
                    subscription.subscription));
            send(new Unsubscribe(requestID, subscription.subscription));
        } else {
            unsubFuture.complete(remainingCount);
        }
        return unsubFuture;
    }

    private CompletableFuture<Publication> reallyPublish(String topic, List<Object> args,
                                                         Map<String, Object> kwargs,
                                                         PublishOptions options) {
        throwIfNotConnected();
        CompletableFuture<Publication> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mPublishRequests.put(requestID, new PublishRequest(requestID, future));
        if (options != null) {
            send(new Publish(requestID, topic, args, kwargs, options.acknowledge, options.excludeMe,
                    options.retain));
        } else {
            send(new Publish(requestID, topic, args, kwargs, true, true, false));
        }
        return future;
    }

    @Override
    public CompletableFuture<Publication> publish(
            String topic,
            List<Object> args,
            Map<String, Object> kwargs,
            PublishOptions options) {
        return reallyPublish(topic, args, kwargs, options);
    }

    @Override
    public CompletableFuture<Publication> publish(
            String topic,
            Object arg,
            PublishOptions options) {
        List<Object> args = new ArrayList<>();
        args.add(arg);
        return reallyPublish(topic, args, null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(
            String topic,
            PublishOptions options,
            Object... args) {
        return reallyPublish(topic, Arrays.asList(args), null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, Object... args) {
        return reallyPublish(topic, Arrays.asList(args), null, null);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, PublishOptions options) {
        return reallyPublish(topic, null, null, options);
    }

    @Override
    public CompletableFuture<Publication> publish(String topic) {
        return reallyPublish(topic, null, null, null);
    }

    private CompletableFuture<Registration> reallyRegister(
            String procedure,
            Object endpoint,
            RegisterOptions options) {
        throwIfNotConnected();
        CompletableFuture<Registration> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mRegisterRequest.put(requestID,
                new RegisterRequest(requestID, future, procedure, endpoint));
        if (options != null) {
            send(new Register(requestID, procedure, options.match, options.invoke));
        } else {
            send(new Register(requestID, procedure, null, null));
        }
        return future;
    }

    @Override
    public <T> CompletableFuture<Registration> register(String procedure, Supplier<T> endpoint) {
        return reallyRegister(procedure, endpoint, null);
    }

    @Override
    public <T> CompletableFuture<Registration> register(
            String procedure,
            Supplier<T> endpoint,
            RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    @Override
    public CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint) {
        return reallyRegister(procedure, endpoint, null);
    }

    @Override
    public CompletableFuture<Registration> register(
            String procedure,
            IInvocationHandler endpoint,
            RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    @Override
    public <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint) {
        return reallyRegister(procedure, endpoint, null);
    }

    @Override
    public <T, R> CompletableFuture<Registration> register(
            String procedure,
            Function<T, R> endpoint,
            RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    @Override
    public <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint) {
        return reallyRegister(procedure, endpoint, null);
    }

    @Override
    public <T, R> CompletableFuture<Registration> register(
            String procedure,
            BiFunction<T, InvocationDetails, R> endpoint,
            RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    @Override
    public <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint) {
        return reallyRegister(procedure, endpoint, null);
    }

    @Override
    public <T, U, R> CompletableFuture<Registration> register(
            String procedure,
            TriFunction<T, U, InvocationDetails, R> endpoint,
            RegisterOptions options) {
        return reallyRegister(procedure, endpoint, options);
    }

    @Override
    public CompletableFuture<Integer> unregister(Registration registration) {
        if (!registration.isActive()) {
            throw new IllegalStateException("Registration is already inactive");
        }
        if (!mRegistrations.containsKey(registration.registration)) {
            throw new IllegalStateException("Not registered");
        }

        CompletableFuture<Integer> unregFuture = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mUnregisterRequests.put(requestID, new UnregisterRequest(requestID, unregFuture,
                registration.registration));
        send(new Unregister(requestID, registration.registration));
        return unregFuture;
    }

    private <T> CompletableFuture<T> reallyCall(
            String procedure,
            List<Object> args, Map<String, Object> kwargs,
            CallOptions options,
            TypeReference<T> resultTypeReference,
            Class<T> resultTypeClass) {
        throwIfNotConnected();

        CompletableFuture<T> future = new CompletableFuture<>();

        long requestID = mIDGenerator.next();

        mCallRequests.put(requestID, new CallRequest(requestID, procedure, future, options,
                resultTypeReference, resultTypeClass));

        if (options == null) {
            send(new Call(requestID, procedure, args, kwargs, 0));
        } else {
            send(new Call(requestID, procedure, args, kwargs, options.timeout));
        }
        return future;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure) {
        return reallyCall(procedure, null, null, null, null, null);
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, null, null, null);
    }

    @Override
    public <T> CompletableFuture<T> call(String procedure, TypeReference<T> resultType) {
        return reallyCall(procedure, null, null, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(String procedure, Class<T> resultType) {
        return reallyCall(procedure, null, null, null, null, resultType);
    }

    @Override
    public CompletableFuture<CallResult> call(
            String procedure,
            CallOptions options,
            Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, options, null, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            TypeReference<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, null, null, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            Class<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, null, null, options, null, resultType);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            TypeReference<T> resultType) {
        return reallyCall(procedure, args, null, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(String procedure, List<Object> args, Class<T> resultType) {
        return reallyCall(procedure, args, null, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            TypeReference<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, args, null, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Class<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, args, null, options, null, resultType);
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, Map<String, Object> kwargs) {
        return reallyCall(procedure, null, kwargs, null, null, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            TypeReference<T> resultType) {
        return reallyCall(procedure, null, kwargs, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            Class<T> resultType) {
        return reallyCall(procedure, null, kwargs, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            TypeReference<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, null, kwargs, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            Map<String, Object> kwargs,
            Class<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, null, kwargs, options, null, resultType);
    }

    @Override
    public CompletableFuture<CallResult> call(
            String procedure,
            Map<String, Object> kwargs,
            CallOptions options) {
        return reallyCall(procedure, null, kwargs, options, null, null);
    }

    @Override
    public CompletableFuture<CallResult> call(
            String procedure,
            List<Object> args,
            Map<String, Object> kwargs,
            CallOptions options) {
        return reallyCall(procedure, args, kwargs, options, null, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Map<String, Object> kwargs,
            TypeReference<T> resultType) {
        return reallyCall(procedure, args, kwargs, null, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Map<String, Object> kwargs,
            Class<T> resultType) {
        return reallyCall(procedure, args, kwargs, null, null, resultType);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Map<String, Object> kwargs,
            TypeReference<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, args, kwargs, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            List<Object> args,
            Map<String, Object> kwargs,
            Class<T> resultType,
            CallOptions options) {
        return reallyCall(procedure, args, kwargs, options, null, resultType);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            TypeReference<T> resultType,
            CallOptions options,
            Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, options, resultType, null);
    }

    @Override
    public <T> CompletableFuture<T> call(
            String procedure,
            TypeReference<T> resultType,
            Object... args) {
        return reallyCall(procedure, Arrays.asList(args), null, null, resultType, null);
    }

    private CompletableFuture<SessionDetails> reallyJoin(
            String realm,
            List<IAuthenticator> authenticators) {
        LOGGER.d("Called join() with realm=" + realm);
        mRealm = realm;
        mAuthenticators = authenticators;
        mGoodbyeSent = false;
        Map<String, Map> roles = new HashMap<>();
        roles.put("publisher", new HashMap<>());
        roles.put("subscriber", new HashMap<>());
        roles.put("caller", new HashMap<>());
        roles.put("callee", new HashMap<>());
        if (mAuthenticators == null) {
            send(new Hello(realm, roles));
        } else {
            List<String> authMethods = new ArrayList<>();
            String authID = null;
            Map<String, Object> authextra = null;
            for (IAuthenticator authenticator: mAuthenticators) {
                authMethods.add(authenticator.getAuthMethod());
                if (authenticator.getAuthMethod().equals(TicketAuth.authmethod)) {
                    TicketAuth auth = (TicketAuth) authenticator;
                    authID = auth.authid;
                } else if (authenticator.getAuthMethod().equals(ChallengeResponseAuth.authmethod)) {
                    ChallengeResponseAuth auth = (ChallengeResponseAuth) authenticator;
                    authID = auth.authid;
                } else if (authenticator.getAuthMethod().equals(CryptosignAuth.authmethod)) {
                    CryptosignAuth auth = (CryptosignAuth) authenticator;
                    authID = auth.authid;
                    authextra = auth.authextra;
                }
            }
            send(new Hello(realm, roles, authMethods, authID, authextra));
        }
        mJoinFuture = new CompletableFuture<>();
        mState = STATE_HELLO_SENT;
        return mJoinFuture;
    }

    @Override
    public CompletableFuture<SessionDetails> join(String realm) {
        return reallyJoin(realm, null);
    }

    @Override
    public CompletableFuture<SessionDetails> join(
            String realm,
            List<IAuthenticator> authenticators) {
        return reallyJoin(realm, authenticators);
    }

    @Override
    public void leave() {
        leave(null, null);
    }

    @Override
    public void leave(String reason) {
        leave(reason, null);
    }

    @Override
    public void leave(String reason, String message) {
        LOGGER.d(String.format("reason=%s message=%s", reason, message));
        send(new Goodbye(reason, message));
        mState = STATE_GOODBYE_SENT;
    }

    public OnJoinListener addOnJoinListener(OnJoinListener listener) {
        return addListener(mOnJoinListeners, listener);
    }

    public void removeOnJoinListener(OnJoinListener listener) {
        removeListener(mOnJoinListeners, listener);
    }

    @Deprecated
    public OnReadyListener adOnReadyListener(OnReadyListener listener) {
        return addOnReadyListener(listener);
    }

    public OnReadyListener addOnReadyListener(OnReadyListener listener) {
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

    public ReflectionServices getReflectionServices() {
        return mReflectionServices;
    }
}
