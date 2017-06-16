package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.List;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Unregister implements IMessage {

    public static final int MESSAGE_TYPE = 66;
    private final long request;
    private final long registration;

    public Unregister(long request, long registration) {
        this.request = request;
        this.registration = registration;
    }

    public static Unregister parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for UNREGISTER", wmsg.size()));
        }

        return new Unregister((long) wmsg.get(1), (long) wmsg.get(2));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        marshaled.add(registration);
        return marshaled;
    }
}
