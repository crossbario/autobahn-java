package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.Cast;

public class Interrupt implements IMessage {

    public static final int MESSAGE_TYPE = 69;

    private static final String ABORT = "abort";
    private static final String KILL = "kill";

    public final long request;
    public final String mode;

    public Interrupt(long request, String mode) {
        this.request = request;
        this.mode = mode;
    }

    public static Interrupt parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for INTERRUPT", wmsg.size()));
        }

        long request = Cast.castRequestID(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String mode = (String) options.getOrDefault("mode", null);
        if (mode != null) {
            if (!Objects.equals(mode, ABORT) && !Objects.equals(mode, KILL)) {
                throw new ProtocolError(String.format("invalid value %s for 'mode' option in INTERRUPT", mode));
            }
        }
        return new Interrupt(request, mode);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (mode != null) {
            Map<String, Object> options = new HashMap<>();
            options.put("mode", mode);
            marshaled.add(options);
        } else {
            // Empty options as third item.
            marshaled.add(new HashMap<>());
        }
        return marshaled;
    }
}
