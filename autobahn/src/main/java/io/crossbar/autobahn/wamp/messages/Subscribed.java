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

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Subscribed implements IMessage {

    public static final int MESSAGE_TYPE = 33;
    public final long request;
    public final long subscription;

    public Subscribed(long request, long subscription) {
        this.request = request;
        this.subscription = subscription;
    }

    public static Subscribed parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "SUBSCRIBED", 3);
        return new Subscribed(MessageUtil.parseLong(wmsg.get(1)), MessageUtil.parseLong(wmsg.get(2)));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        marshaled.add(subscription);
        return marshaled;
    }
}
