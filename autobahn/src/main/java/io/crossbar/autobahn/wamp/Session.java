package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ComponentConfig;
import io.crossbar.autobahn.wamp.types.Hello;
import io.crossbar.autobahn.wamp.types.IEventHandler;
import io.crossbar.autobahn.wamp.types.IInvocationHandler;
import io.crossbar.autobahn.wamp.types.Message;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;


public class Session implements ISession, ITransportHandler {

    private ITransport mTransport;

    private ArrayList<OnJoinListener> mOnJoinListeners;
    private ArrayList<OnLeaveListener> mOnLeaveListeners;
    private ArrayList<OnConnectListener> mOnConnectListeners;
    private ArrayList<OnDisconnectListener> mOnDisconnectListeners;
    private ArrayList<OnUserErrorListener> mOnUserErrorListeners;

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
    }

    @Override
    public void onMessage(Message message) {
        // process the incoming WAMP message
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
        return null;
    }

    @Override
    public CompletableFuture<Publication> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                  PublishOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint,
                                                    RegisterOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        return null;
    }

    @Override
    public void join(String realm, List<String> authMethods, String authID, String authRole,
                     Map<String, Object> authExtra, boolean resumable, int resumeSession, String resumeToken) {
        mRealm = realm;
        Map<String, Map> roles = new HashMap<>();
        roles.put("publisher", new HashMap<>());
        mTransport.send(new Hello(realm, roles));
    }

    @Override
    public void leave(String reason, String message) {
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
