package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.Cast;

public class Subscribe implements IMessage {

    public static final int MESSAGE_TYPE = 32;

    private final long request;
    private final String topic;
    private final String match;
    private final boolean getRetained;

    private final String MATCH_EXACT = "exact";
    private final String MATCH_PREFIX = "prefix";
    private final String MATCH_WILDCARD = "wildcard";

    public Subscribe(long request, String topic, String match, boolean getRetained) {
        this.request = request;
        this.topic = topic;
        if (match != null) {
            if (!Objects.equals(match, MATCH_EXACT) && !Objects.equals(match, MATCH_PREFIX) &&
                    !Objects.equals(match, MATCH_WILDCARD)) {
                throw new IllegalArgumentException("match must be one of exact, prefix or wildcard.");
            }
            this.match = match;
        } else {
            this.match = MATCH_EXACT;
        }
        this.getRetained = getRetained;
    }

    public static Subscribe parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 4) {
            throw new ProtocolError(String.format("invalid message length %s for SUBSCRIBE", wmsg.size()));
        }

        long request = Cast.castRequestID(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String match = (String) options.get("match");
        boolean getRetained = (boolean) options.get("get_retained");
        String topic = (String) wmsg.get(3);
        return new Subscribe(request, topic, match,getRetained);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> extra = new HashMap<>();
        extra.put("match", match);
        extra.put("get_retained", getRetained);
        marshaled.add(extra);
        marshaled.add(topic);
        return marshaled;
    }
}
