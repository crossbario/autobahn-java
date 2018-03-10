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
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Register implements IMessage {

    public static final int MESSAGE_TYPE = 64;

    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_PREFIX = "prefix";
    private static final String MATCH_WILDCARD = "wildcard";

    private final static String INVOKE_SINGLE = "single";
    private final static String INVOKE_FIRST = "first";
    private final static String INVOKE_LAST = "last";
    private final static String INVOKE_ROUNDROBIN = "roundrobin";
    private final static String INVOKE_RANDOM = "random";
    private final static String INVOKE_ALL = "all";

    private final long request;
    private final String procedure;
    private final String match;
    private final String invoke;

    public Register(long request, String procedure, String match, String invoke) {
        this.request = request;
        this.procedure = procedure;
        if (match != null) {
            if (!match.equals(MATCH_EXACT) && !match.equals(MATCH_PREFIX) &&
                    !match.equals(MATCH_WILDCARD)) {
                throw new IllegalArgumentException("match must be one of exact, prefix or wildcard.");
            }
            this.match = match;
        } else {
            this.match = null;
        }
        if (invoke != null) {
            if (!invoke.equals(INVOKE_SINGLE) && !invoke.equals(INVOKE_FIRST) &&
                    !invoke.equals(INVOKE_LAST) && !invoke.equals(INVOKE_ROUNDROBIN) &&
                    !invoke.equals(INVOKE_RANDOM) && !invoke.equals(INVOKE_ALL)) {
                throw new IllegalArgumentException(
                        "invoke must be one of single, first, last, roundrobin, random or all.");
            }
            this.invoke = invoke;
        } else {
            this.invoke = null;
        }
    }

    public static Register parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "REGISTER", 4);

        long request = MessageUtil.parseLong(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String match = null;
        if (options.containsKey("match")) {
            match = (String) options.get("match");
            if (!match.equals(MATCH_EXACT) && !match.equals(MATCH_PREFIX) &&
                    !match.equals(MATCH_WILDCARD)) {
                throw new ProtocolError("match must be one of exact, prefix or wildcard.");
            }
        }
        String invoke = null;
        if (options.containsKey("invoke")) {
            invoke = (String) options.get("invoke");
            if (!invoke.equals(INVOKE_SINGLE) && !invoke.equals(INVOKE_FIRST) &&
                    !invoke.equals(INVOKE_LAST) && !invoke.equals(INVOKE_ROUNDROBIN) &&
                    !invoke.equals(INVOKE_RANDOM) && !invoke.equals(INVOKE_ALL)) {
                throw new IllegalArgumentException(
                        "invoke must be one of single, first, last, roundrobin, random or all.");
            }
        }
        String procedure = (String) wmsg.get(3);
        return new Register(request, procedure, match, invoke);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> options = new HashMap<>();
        if (match != null && !match.equals(MATCH_EXACT)) {
            options.put("match", match);
        }
        if (invoke != null && !invoke.equals(INVOKE_SINGLE)) {
            options.put("invoke", invoke);
        }
        marshaled.add(options);
        marshaled.add(procedure);
        return marshaled;
    }
}
