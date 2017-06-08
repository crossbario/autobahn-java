package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.Challenge;
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

    void join(String realm, List<String> authMethods, String authID, String authRole, Map<String, Object> authExtra,
              boolean resumable, int resumeSession, String resumeToken);

    void leave(String reason, String message);

    void disconnect();

    boolean isConnected();

    boolean isAttached();

    void define(Exception exception, String error);

    void attachTransport(ITransport transport);

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

    interface OnChallengeListener {
        void onChallenge(Challenge challenge);
    }

    // FIXME: come up with an equivalent of txaio.IFailedFuture as first arg.
    interface OnUserErrorListener {
        void onUserError(String message);
    }
}
