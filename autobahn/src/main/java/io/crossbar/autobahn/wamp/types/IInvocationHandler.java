package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IInvocationHandler {
    CompletableFuture<InvocationResult> run(List<Object> args, Map<String, Object> kwargs, InvocationDetails details);
}
