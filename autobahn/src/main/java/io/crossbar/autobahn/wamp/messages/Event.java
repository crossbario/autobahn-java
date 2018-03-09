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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Event implements IMessage {

    public static final int MESSAGE_TYPE = 36;

    public final long subscription;
    public final long publication;
    public final String topic;
    public final boolean retained;
    public final List<Object> args;
    public final Map<String, Object> kwargs;

    public Event(long subscription, long publication, String topic, boolean retained, List<Object> args, Map<String, Object> kwargs) {
        this.subscription = subscription;
        this.publication = publication;
        this.topic = topic;
        this.retained = retained;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Event parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "EVENT", 3, 6);
        long subscription = MessageUtil.parseLong(wmsg.get(1));
        long publication = MessageUtil.parseLong(wmsg.get(2));

        Map<String, Object> details = (Map<String, Object>) wmsg.get(3);
        String topic = (String)details.get("topic");
        boolean retained = getOrDefault(details, "retained", false);

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

        return new Event(subscription, publication, topic, retained, args, kwargs);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(subscription);
        marshaled.add(publication);
        Map<String, Object> details = new HashMap<>();
        if (topic != null) {
            details.put("topic", topic);
        }
        if (retained) {
            details.put("retained", retained);
        }
        marshaled.add(details);
        if (kwargs != null) {
            if (args == null) {
                // Empty args.
                marshaled.add(Collections.emptyList());
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
