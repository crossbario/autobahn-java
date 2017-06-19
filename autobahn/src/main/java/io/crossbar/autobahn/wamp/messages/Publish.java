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

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.Cast;

public class Publish implements IMessage {

    public static final int MESSAGE_TYPE = 16;
    
    public final long request;
    public final String topic;
    public final List<Object> args;
    public final Map<String, Object> kwargs;
    public final boolean acknowledge;
    public final boolean excludeMe;

    public Publish(long request, String topic, List<Object> args, Map<String, Object> kwargs,
                   boolean acknowledge, boolean excludeMe) {

        this.request = request;
        this.topic = topic;
        this.args = args;
        this.kwargs = kwargs;
        this.acknowledge = acknowledge;
        this.excludeMe = excludeMe;
    }

    public static Publish parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() < 4 || wmsg.size() > 6) {
            throw new ProtocolError(String.format("invalid message length %s for PUBLISH", wmsg.size()));
        }

        long request = Cast.castRequestID(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String topic = (String) wmsg.get(3);

        List<Object> args = null;
        if (wmsg.size() > 4) {
            if (wmsg.get(4) instanceof byte[]) {
                throw new ProtocolError("Binary payload not supported");
            }
            args = (List<Object>) wmsg.get(4);
        }

        Map<String, Object> kwargs = null;
        if (wmsg.size() > 5) {
            kwargs = (Map<String, Object>) wmsg.get(5);
        }

        boolean acknowledge = false;
        if (options.containsKey("acknowledge")) {
            acknowledge = (boolean) options.get("acknowledge");
        }

        boolean excludeMe = false;
        if (options.containsKey("exclude_me")) {
            excludeMe = (boolean) options.get("exclude_me");
        }

        return new Publish(request, topic, args, kwargs, acknowledge, excludeMe);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> options = new HashMap<>();
        options.put("acknowledge", acknowledge);
        options.put("exclude_me", excludeMe);
        marshaled.add(options);
        marshaled.add(topic);
        if (kwargs != null) {
            if (args == null) {
                // Empty args.
                marshaled.add(new ArrayList<String>());
            } else {
                marshaled.add(args);
            }
            marshaled.add(kwargs);
        } else if (args != null) {
            marshaled.add(args);
        }
        return marshaled;
    }
}
