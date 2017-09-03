///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Subscribe implements IMessage {

    public static final int MESSAGE_TYPE = 32;

    private final long request;
    private final String topic;
    private final String match;
    private final boolean getRetained;

    private final String MATCH_EXACT = "exact";
    private final String MATCH_PREFIX = "prefix";
    private final String MATCH_WILDCARD = "wildcard";

    public Subscribe(long request, SubscribeOptions options, String topic) {
        this.request = request;
        this.topic = topic;
        if (options != null) {
            if (options.match != null) {
                if (!Objects.equals(options.match, MATCH_EXACT) && !Objects.equals(options.match, MATCH_PREFIX) &&
                        !Objects.equals(options.match, MATCH_WILDCARD)) {
                    throw new IllegalArgumentException("match must be one of exact, prefix or wildcard.");
                }
            }
            this.match = options.match;
            this.getRetained = options.getRetained;
        } else {
            this.match = MATCH_EXACT;
            this.getRetained = false;
        }
    }

    public static Subscribe parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "SUBSCRIBE", 4);

        long request = MessageUtil.parseRequestID(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String match = (String) options.get("match");
        boolean getRetained = (boolean) options.get("get_retained");
        String topic = (String) wmsg.get(3);
        SubscribeOptions opt = new SubscribeOptions(match, true, getRetained);
        return new Subscribe(request, opt, topic);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> extra = new HashMap<>();
        if (match != null && !Objects.equals(match, MATCH_EXACT)) {
        	 extra.put("match", match);
        }
        if (getRetained) {
        	extra.put("get_retained", getRetained);
        }
        marshaled.add(extra);
        marshaled.add(topic);
        return marshaled;
    }
}
