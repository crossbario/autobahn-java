package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;

public interface Progress {
    void sendProgress(List<Object> args, Map<String, Object> kwargs);
}
