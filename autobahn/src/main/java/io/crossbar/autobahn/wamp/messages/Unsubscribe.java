package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.List;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Unsubscribe implements IMessage {

    public static final int MESSAGE_TYPE = 34;
    private final long request;
    private final long subscription;

    public Unsubscribe(long request, long subscription) {
        this.request = request;
        this.subscription = subscription;
    }

    public static Unsubscribe parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for UNSUBSCRIBE", wmsg.size()));
        }
        return new Unsubscribe((long) wmsg.get(1), (long) wmsg.get(2));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        marshaled.add(subscription);
        return marshaled;
    }
}
