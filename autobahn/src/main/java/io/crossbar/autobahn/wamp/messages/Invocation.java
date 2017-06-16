package io.crossbar.autobahn.wamp.messages;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Invocation implements IMessage {

    public static final int MESSAGE_TYPE = 68;

    public static Invocation parse(List<Object> wmsg) {
        return null;
    }

    @Override
    public List<Object> marshal() {
        return null;
    }
}
