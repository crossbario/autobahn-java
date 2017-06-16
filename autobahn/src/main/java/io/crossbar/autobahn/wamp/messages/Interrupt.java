package io.crossbar.autobahn.wamp.messages;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Interrupt implements IMessage {

    public static final int MESSAGE_TYPE = 69;

    private final long request;
    private final String mode;

    public Interrupt(long request, String mode) {
        this.request = request;
        this.mode = mode;
    }

    public static Interrupt parse(List<Object> wmsg) {
        return null;
    }

    @Override
    public List<Object> marshal() {
        return null;
    }
}
