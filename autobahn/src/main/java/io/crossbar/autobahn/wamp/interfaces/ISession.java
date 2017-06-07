package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.IInvocationHandler;
import io.crossbar.autobahn.wamp.types.IEventHandler;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;

public interface ISession {

    CompletableFuture<Subscription> subscribe(String topic, IEventHandler handler, SubscribeOptions options);

    CompletableFuture<Publication> publish(String topic,
                                           List<Object> args,
                                           Map<String, Object> kwargs,
                                           PublishOptions options);

    CompletableFuture<Registration> register(String procedure, IInvocationHandler endpoint, RegisterOptions options);

    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    interface OnJoinListener {
        void onJoin(SessionDetails details);
    }

    interface OnLeaveListener {
        void onLeave(CloseDetails details);
    }

    interface OnConnectListener {
        void onConnect();
    }

    interface OnDisconnectListener {
        void onDisconnect();
    }
}
