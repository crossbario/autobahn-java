package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Cancel implements IMessage {

    public static final int MESSAGE_TYPE = 49;

    private static final String SKIP = "skip";
    private static final String ABORT = "abort";
    private static final String KILL = "kill";

    private final long request;
    private final String mode;

    public Cancel(long request, String mode) {
        this.request = request;
        if (mode != null) {
            if (!Objects.equals(mode, SKIP) && !Objects.equals(mode, ABORT) && !Objects.equals(mode, KILL)) {
                throw new IllegalArgumentException("mode must either be skip, abort or kill");
            }
        }
        this.mode = mode;
    }

    public static Cancel parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for CANCEL", wmsg.size()));
        }

        long request = (long) wmsg.get(1);
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String mode = (String) options.getOrDefault("mode", null);
        return new Cancel(request, mode);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (mode != null) {
            Map<String, Object> options = new HashMap<>();
            options.put("mode", mode);
            marshaled.add(mode);
        }
        return marshaled;
    }
}
