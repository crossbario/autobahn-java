package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.IInvocationHandler;
import io.crossbar.autobahn.wamp.types.IEventHandler;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;

public class Session implements ISession {

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
        endpoint.get(new ArrayList<>(), new HashMap<>());
        return null;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        return null;
    }
}
