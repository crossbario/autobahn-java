package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Unregistered implements IMessage {

    public static final int MESSAGE_TYPE = 67;

    private static final long REGISTRATION_NULL = -1;
    private final long request;
    private final long registration;
    private final String reason;

    public Unregistered(long request, long registration, String reason) {
        this.request = request;
        this.registration = registration;
        this.reason = reason;
    }

    public static Unsubscribed parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() < 2 || wmsg.size() > 3) {
            throw new ProtocolError(String.format("invalid message length %s for UNSUBSCRIBED", wmsg.size()));
        }

        long request = (long) wmsg.get(1);

        long registration = REGISTRATION_NULL;
        String reason = null;
        if (wmsg.size() > 2) {
            Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
            registration = (long) details.getOrDefault("registration", registration);
            reason = (String) details.getOrDefault("reason", reason);
        }

        return new Unsubscribed(request, registration, reason);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (registration != REGISTRATION_NULL || reason != null) {
            Map<String, Object> details = new HashMap<>();
            if (reason != null) {
                details.put("reason", reason);
            }
            if (registration != REGISTRATION_NULL) {
                details.put("registration", registration);
            }
            marshaled.add(details);
        }
        return marshaled;
    }
}
