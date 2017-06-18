package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.messages.Call;
import io.crossbar.autobahn.wamp.messages.Event;
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
import io.crossbar.autobahn.wamp.messages.Goodbye;
import io.crossbar.autobahn.wamp.requests.CallRequest;
import io.crossbar.autobahn.wamp.requests.PublishRequest;
import io.crossbar.autobahn.wamp.requests.RegisterRequest;
import io.crossbar.autobahn.wamp.requests.SubscribeRequest;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ComponentConfig;
import io.crossbar.autobahn.wamp.types.IEventHandler;
import io.crossbar.autobahn.wamp.types.IInvocationHandler;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.crossbar.autobahn.wamp.utils.IDGenerator;


public class Session implements ISession, ITransportHandler {

    private final int STATE_DISCONNECTED = 1;
    private final int STATE_HELLO_SENT = 2;
    private final int STATE_AUTHENTICATE_SENT = 3;
    private final int STATE_JOINED = 4;
    private final int STATE_READY = 5;
    private final int STATE_GOOBYE_SENT = 6;
    private final int STATE_ABORT_SENT = 7;

    private ITransport mTransport;

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

    private ComponentConfig mComponentConfig;

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

    public Session(ComponentConfig config) {
        this();
        mComponentConfig = config;
    }

    @Override
    public void onConnect(ITransport transport) {
        System.out.println("Session.onConnect");
        if (mTransport != null) {
            // Now allowed to throw here, find a better way.
//            throw new Exception("already connected");
        }
        mTransport = transport;
        mOnConnectListeners.forEach(OnConnectListener::onConnect);
    }

    @Override
    public void onMessage(IMessage message) {
        if (mSessionID == 0) {
            if (message instanceof Welcome) {
                mState = STATE_JOINED;
                Welcome msg = (Welcome) message;
                mSessionID = msg.session;
                SessionDetails details = new SessionDetails(msg.realm, msg.session);
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnJoinListeners.forEach(l -> futures.add(CompletableFuture.runAsync(() -> l.onJoin(details))));
                CompletableFuture d = combineFutures(futures);
                d.thenRun(() -> {
                    mState = STATE_READY;
                    mOnReadyListeners.forEach(OnReadyListener::onReady);
                });
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
                    request.onReply.complete(new CallResult(msg.args, msg.kwargs));
                } else {
                    // throw some exception.
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
                    // throw some exception.
                }
            } else if (message instanceof Event) {
                Event msg = (Event) message;
                List<Subscription> subscriptions = mSubscriptions.getOrDefault(msg.subscription, null);
                if (subscriptions != null) {
                    subscriptions.forEach(s -> s.handler.run(msg.args, msg.kwargs));
                    List<CompletableFuture<?>> futures = new ArrayList<>();
                    subscriptions.forEach(
                            s -> futures.add(CompletableFuture.runAsync(() -> s.handler.run(msg.args, msg.kwargs))));
                    combineFutures(futures);
                } else {
                    // throw some exception.
                }
            } else if (message instanceof Published) {
                Published msg = (Published) message;
                PublishRequest request = mPublishRequests.getOrDefault(msg.request, null);
                if (request != null) {
                    mPublishRequests.remove(msg.request);
                    Publication publication = new Publication(msg.publication);
                    request.onReply.complete(publication);
                } else {
                    // throw some exception.
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
                    // throw some exception.
                }
            } else if (message instanceof Invocation) {
                Invocation msg = (Invocation) message;
                Registration registration = mRegistrations.getOrDefault(msg.registration, null);
                if (registration != null) {
                    CompletableFuture.runAsync(() -> registration.endpoint.run(msg.args, msg.kwargs, null));
                } else {
                    // throw some exception.
                }
            } else if (message instanceof Goodbye) {

                CloseDetails details = new CloseDetails();
                List<CompletableFuture<?>> futures = new ArrayList<>();
                mOnLeaveListeners.forEach(l -> futures.add(CompletableFuture.runAsync(() -> l.onLeave(details))));
                CompletableFuture d = combineFutures(futures);
                d.thenRun(() -> {
                    System.out.println("CLOSED NOW");
                    mState = STATE_DISCONNECTED;
                    if (mTransport != null && mTransport.isOpen()) {
                        mTransport.close();
                    }
                });

            } else {
                System.out.println("FIXME (session " + mSessionID + "): unprocessed message:");
                System.out.println(message);
            }
        }
    }

    @Override
    public void onDisconnect(boolean wasClean) {
        System.out.println("Session.onDisconnect");

        List<CompletableFuture<?>> futures = new ArrayList<>();
        mOnDisconnectListeners.forEach(l -> futures.add(CompletableFuture.runAsync(() -> l.onDisconnect(wasClean))));
        CompletableFuture d = combineFutures(futures);
        d.thenRun(() -> {
            System.out.println("DISCONNECTED NOW");
            mTransport = null;
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
        mTransport.send(new Subscribe(requestID, options, topic));
        return future;
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                  PublishOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
        CompletableFuture<Publication> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mPublishRequests.put(requestID, new PublishRequest(requestID, future));
        if (options != null) {
            mTransport.send(new Publish(requestID, topic, args, kwargs, options.acknowledge, options.excludeMe));
        } else {
            mTransport.send(new Publish(requestID, topic, args, kwargs, true, true));
        }
        return future;
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
            mTransport.send(new Register(requestID, procedure, options.match, options.invoke));
        } else {
            mTransport.send(new Register(requestID, procedure, null, null));
        }
        return future;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        if (!isConnected()) {
            throw new IllegalStateException("The transport must be connected first");
        }
        CompletableFuture<CallResult> future = new CompletableFuture<>();
        long requestID = mIDGenerator.next();
        mCallRequests.put(requestID, new CallRequest(requestID, procedure, future, options));
        if (options == null) {
            mTransport.send(new Call(requestID, procedure, args, kwargs, 0));
        } else {
            mTransport.send(new Call(requestID, procedure, args, kwargs, options.timeout));
        }
        return future;
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
        mTransport.send(new Hello(realm, roles));
        mState = STATE_HELLO_SENT;
        return null;
    }

    @Override
    public void leave(String reason, String message) {
        System.out.println("Session.leave");
        mTransport.send(new Goodbye(null, null));
        mState = STATE_GOOBYE_SENT;
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
