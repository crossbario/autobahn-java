package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IEventHandler<R> {
    R onHandle(List<Object> args, Map<String, Object> kwargs);
}
