package io.crossbar.autobahn.wamp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.IEndpoint;
import io.crossbar.autobahn.wamp.types.ISubscribeHandler;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.PublicationResult;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;

public class Session implements ISession {

    @Override
    public CompletableFuture<Subscription> subscribe(ISubscribeHandler consumer, String topic,
                                                     SubscribeOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<PublicationResult> publish(String topic, List<Object> args, Map<String, Object> kwargs,
                                                        PublishOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<Registration> register(IEndpoint consumer, String procedure,
                                                    RegisterOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<CallResult> call(String procedure, List<Object> args, Map<String, Object> kwargs,
                                              CallOptions options) {
        return null;
    }
}
