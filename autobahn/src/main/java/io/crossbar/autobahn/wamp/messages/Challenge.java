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
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Challenge implements IMessage {

    public static final int MESSAGE_TYPE = 4;

    public final String method;
    public final Map<String, Object> extra;

    public Challenge(String method, Map<String, Object> extra) {
        this.method = method;
        this.extra = extra;
    }

    public static Challenge parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "CHALLENGE", 3);
        return new Challenge((String) wmsg.get(1), (Map<String, Object>) wmsg.get(2));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(method);
        marshaled.add(extra);
        return marshaled;
    }
}
