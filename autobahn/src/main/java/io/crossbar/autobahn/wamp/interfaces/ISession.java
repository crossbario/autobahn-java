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
    int STATE_CONNECTED = 1;
    int STATE_CONNECTED_RESUMING = 2;
    int STATE_DISCONNECTED = 3;
    int STATE_DISCONNECTED_RESUMABLE = 4;
    int STATE_HELLO_SENT = 5;
    int STATE_AUTHENTICATE_SENT = 6;
    int STATE_JOINED = 7;
    int STATE_READY = 8;
    int STATE_GOODBYE_SENT = 9;
    int STATE_SENT = 10;

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
