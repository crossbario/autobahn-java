package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Goodbye implements IMessage {

    public static final int MESSAGE_TYPE = 6;

    private final String DEFAULT_REASON = "wamp.close.normal";

    public final String reason;
    public final String message;

    public Goodbye(String reason, String message) {
        if (reason == null) {
            this.reason = DEFAULT_REASON;
        } else {
            this.reason = reason;
        }
        this.message = message;
    }

    public static Goodbye parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for GOODBYE", wmsg.size()));
        }
        Map<String, Object> details = (Map<String, Object>) wmsg.get(1);
        String message = null;
        if (details.containsKey("message")) {
            message = (String) details.get("message");
        }
        return new Goodbye((String) wmsg.get(2), message);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        Map<String, Object> details = new HashMap<>();
        if (message != null) {
            details.put("message", message);
        }
        marshaled.add(details);
        marshaled.add(reason);
        return marshaled;
    }
}
