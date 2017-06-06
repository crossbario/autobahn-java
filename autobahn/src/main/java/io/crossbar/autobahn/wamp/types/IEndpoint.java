package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IEndpoint<R> {
    // Tentative name, to be changed.
    R get(List<Object> args, Map<String, Object> kwargs);
}
