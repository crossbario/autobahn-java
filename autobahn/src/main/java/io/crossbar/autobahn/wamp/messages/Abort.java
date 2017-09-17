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

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Abort implements IMessage {

    public static final int MESSAGE_TYPE = 3;

    public final String reason;
    public final String message;

    public Abort(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    public static Abort parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "ABORT", 3);

        Map<String, Object> details = (Map<String, Object>) wmsg.get(1);
        String message = getOrDefault(details, "message", null);
        String reason = (String) wmsg.get(2);

        return new Abort(reason, message);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        Map<String, Object> details = new HashMap<>();
        if (message != null) {
            details.put("message", message);
        }
        marshaled.add(details);
        marshaled.add(reason);
        return marshaled;
    }
}
