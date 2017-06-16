package io.crossbar.autobahn.wamp.messages;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Yield implements IMessage {

    public static final int MESSAGE_TYPE = 70;

    private final long request;
    private final List<Object> args;
    private final Map<String, Object> kwargs;

    public Yield(long request, List<Object> args, Map<String, Object> kwargs) {
        this.request = request;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Yield parse(List<Object> wmsg) {
        return null;
    }

    @Override
    public List<Object> marshal() {
        return null;
    }
}
