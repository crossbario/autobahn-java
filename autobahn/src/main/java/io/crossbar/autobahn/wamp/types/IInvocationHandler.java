package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

//@FunctionalInterface
//public interface IInvocationHandler {
//    CompletableFuture<InvocationResult> run(List<Object> args, Map<String, Object> kwargs, InvocationDetails details);
//}

@FunctionalInterface
public interface IInvocationHandler<R> {
    // This is a signature for the callback that the user code will provide.
    R run(List<Object> args, Map<String, Object> kwargs, InvocationDetails details);
}
