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

public class Unsubscribed implements IMessage {

    public static final int MESSAGE_TYPE = 35;

    private static final long SUBSCRIPTION_NULL = -1;
    public final long request;
    public final long subscription;
    public final String reason;

    public Unsubscribed(long request, long subscription, String reason) {
        this.request = request;
        this.subscription = subscription;
        this.reason = reason;
    }

    public static Unsubscribed parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "UNSUBSCRIBED", 2, 3);

        long request = MessageUtil.parseLong(wmsg.get(1));

        long subscription = SUBSCRIPTION_NULL;
        String reason = null;
        if (wmsg.size() > 2) {
            Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
            if (details.containsKey("subscription")) {
                subscription = MessageUtil.parseLong(details.get("subscription"));
            }
            if (details.containsKey("reason")) {
                reason = (String) details.get("reason");
            }
        }

        return new Unsubscribed(request, subscription, reason);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (subscription != SUBSCRIPTION_NULL || reason != null) {
            Map<String, Object> details = new HashMap<>();
            if (reason != null) {
                details.put("reason", reason);
            }
            if (subscription != SUBSCRIPTION_NULL) {
                details.put("subscription", subscription);
            }
            marshaled.add(details);
        }
        return marshaled;
    }
}
