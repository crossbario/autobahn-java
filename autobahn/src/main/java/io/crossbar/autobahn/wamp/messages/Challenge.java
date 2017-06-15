package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Challenge implements IMessage {

    public static final int MESSAGE_TYPE = 4;

    public final String method;
    public final Map<String, Object> extra;

    public Challenge(String method, Map<String, Object> extra) {
        this.method = method;
        this.extra = extra;
    }

    public static Challenge parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for CHALLENGE", wmsg.size()));
        }
        return new Challenge((String) wmsg.get(1), (Map<String, Object>) wmsg.get(2));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(method);
        marshaled.add(extra);
        return marshaled;
    }
}
