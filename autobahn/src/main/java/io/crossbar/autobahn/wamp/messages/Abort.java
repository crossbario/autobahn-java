package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Abort implements IMessage {

    public static final int MESSAGE_TYPE = 3;

    public final String reason;
    public final String message;

    public Abort(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    public static Abort parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for ABORT", wmsg.size()));
        }

        Map<String, Object> details = (Map<String, Object>) wmsg.get(1);
        String message = null;
        if (details.containsKey("message")) {
            message = (String) details.get("message");
        }
        String reason = (String) wmsg.get(2);

        return new Abort(reason, message);
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
