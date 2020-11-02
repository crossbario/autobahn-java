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

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Subscribe implements IMessage {

    public static final int MESSAGE_TYPE = 32;

    private final long request;
    private final String topic;
    private final SubscribeOptions options;

    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_PREFIX = "prefix";
    private static final String MATCH_WILDCARD = "wildcard";

    public Subscribe(long request, SubscribeOptions options, String topic) {
        this.request = request;
        this.topic = topic;
        if (options != null) {
            String match = options.getMatch();
            if (match != null) {
                if (!match.equals(MATCH_EXACT) && !match.equals(MATCH_PREFIX) &&
                        !match.equals(MATCH_WILDCARD)) {
                    throw new IllegalArgumentException("match must be one of exact, prefix or wildcard.");
                }
            }
            this.options = options;
        } else {
            this.options = new SubscribeOptions(MATCH_EXACT, false);
        }
    }

    public static Subscribe parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "SUBSCRIBE", 4);

        long request = MessageUtil.parseLong(wmsg.get(1));
        SubscribeOptions options = new SubscribeOptions((Map<String, Object>) wmsg.get(2));
        
        String match = options.getMatch();
        if (match != null && !match.equals(MATCH_EXACT) && !match.equals(MATCH_PREFIX) &&
                !match.equals(MATCH_WILDCARD)) {
            throw new ProtocolError("match must be one of exact, prefix or wildcard.");
        }

        String topic = (String) wmsg.get(3);
        return new Subscribe(request, options, topic);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);

        SubscribeOptions options = new SubscribeOptions(this.options);
        String match = options.getMatch();
        if (match != null && match.equals(MATCH_EXACT)) {
        	 options.removeMatch();
        }

        marshaled.add(options);
        marshaled.add(topic);
        return marshaled;
    }
}
