package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.IEndpoint;
import io.crossbar.autobahn.wamp.types.ISubscribeHandler;
import io.crossbar.autobahn.wamp.types.PublicationResult;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.types.Subscription;

public interface ISession {

    CompletableFuture<Subscription> subscribe(ISubscribeHandler handler, String topic, SubscribeOptions options);

    CompletableFuture<PublicationResult> publish(String topic,
                                                 List<Object> args,
                                                 Map<String, Object> kwargs,
                                                 PublishOptions options);

    CompletableFuture<Registration> register(IEndpoint endpoint, String procedure, RegisterOptions options);

    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

}
