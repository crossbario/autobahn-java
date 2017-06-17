package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.messages.Call;
import io.crossbar.autobahn.wamp.messages.Hello;
import io.crossbar.autobahn.wamp.messages.Publish;
import io.crossbar.autobahn.wamp.messages.Register;
import io.crossbar.autobahn.wamp.messages.Subscribe;
import io.crossbar.autobahn.wamp.messages.Welcome;
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
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;


public class Session implements ISession, ITransportHandler {

    private ITransport mTransport;

    private final ArrayList<OnJoinListener> mOnJoinListeners;
    private final ArrayList<OnLeaveListener> mOnLeaveListeners;
    private final ArrayList<OnConnectListener> mOnConnectListeners;
    private final ArrayList<OnDisconnectListener> mOnDisconnectListeners;
    private final ArrayList<OnUserErrorListener> mOnUserErrorListeners;

    private long mSessionID;
    private boolean mGoodbyeSent;
    private String mRealm;

    private ComponentConfig mComponentConfig;

    public Session() {
        mOnJoinListeners = new ArrayList<>();
        mOnLeaveListeners = new ArrayList<>();
        mOnConnectListeners = new ArrayList<>();
        mOnDisconnectListeners = new ArrayList<>();
        mOnUserErrorListeners = new ArrayList<>();
    }

    public Session(ComponentConfig config) {
        this();
        mComponentConfig = config;
    }

    @Override
    public void onConnect(ITransport transport) {
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
                Welcome msg = (Welcome) message;
                mSessionID = msg.session;
                SessionDetails details = new SessionDetails(msg.realm, msg.session);
                mOnJoinListeners.forEach(onJoinListener -> onJoinListener.onJoin(details));
            }
        } else {
            // Now that we have an active session handle all incoming messages here.
            System.out.println(message);
        }
    }

    @Override
    public void onDisconnect(boolean wasClean) {
        if (mTransport == null) {
            // Now allowed to throw here, find a better way.
//            throw new Exception("not connected");
        }
        mTransport = null;
    }

    @Override
    public boolean isConnected() {
        return mTransport != null;
    }

    @Override
    public CompletableFuture<Subscription> subscribe(String topic, IEventHandler handler, SubscribeOptions options) {
        CompletableFuture<Subscription> future = new CompletableFuture<>();
        long requestID = getRandomNumber();
        mTransport.send(new Subscribe(requestID, topic, null, false));
        return future;
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                  PublishOptions options) {
        CompletableFuture<Publication> future = new CompletableFuture<>();
        long requestID = getRandomNumber();
        mTransport.send(new Publish(requestID, topic, args, kwargs, false, true));
        return future;
    }

    @Override
    public CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint,
                                                    RegisterOptions options) {
        CompletableFuture<Registration> future = new CompletableFuture<>();
        long requestID = getRandomNumber();
        mTransport.send(new Register(requestID, procedure, null, null));
        return future;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        CompletableFuture<CallResult> future = new CompletableFuture<>();
        long requestID = getRandomNumber();
        mTransport.send(new Call(requestID, procedure, args, kwargs));
        return future;
    }

    @Override
    public CompletableFuture<SessionDetails> join(String realm, List<String> authMethods) {
        mRealm = realm;
        mGoodbyeSent = false;
        Map<String, Map> roles = new HashMap<>();
        roles.put("publisher", new HashMap<>());
        roles.put("subscriber", new HashMap<>());
        roles.put("caller", new HashMap<>());
        roles.put("callee", new HashMap<>());
        mTransport.send(new Hello(realm, roles));
        return null;
    }

    @Override
    public void leave(String reason, String message) {

    }

    private long getRandomNumber() {
        return ThreadLocalRandom.current().nextLong(0,9007199254740992L);
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
