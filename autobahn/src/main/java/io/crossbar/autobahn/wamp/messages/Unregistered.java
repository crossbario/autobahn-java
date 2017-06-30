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

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

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

    public static Unregistered parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "UNREGISTERED", 2, 3);

        long registration = REGISTRATION_NULL;
        String reason = null;
        if (wmsg.size() > 2) {
            Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
            registration = (long) details.getOrDefault("registration", registration);
            reason = (String) details.getOrDefault("reason", reason);
        }

        return new Unregistered(MessageUtil.parseRequestID(wmsg.get(1)), registration, reason);
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
