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

public class Authenticate implements IMessage {

    public static final int MESSAGE_TYPE = 5;

    public final String signature;
    public final Map<String, Object> extra;

    public Authenticate(String signature, Map<String, Object> extra) {
        this.signature = signature;
        this.extra = extra;
    }

    public static Authenticate parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "AUTHENTICATE", 3);
        return new Authenticate((String) wmsg.get(1), (Map<String, Object>) wmsg.get(2));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(signature);
        if (extra == null) {
            marshaled.add(new HashMap<>());
        } else {
            marshaled.add(extra);
        }
        return marshaled;
    }
}
