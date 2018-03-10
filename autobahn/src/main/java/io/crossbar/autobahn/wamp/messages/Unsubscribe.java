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

public class Unsubscribe implements IMessage {

    public static final int MESSAGE_TYPE = 34;
    private final long request;
    private final long subscription;

    public Unsubscribe(long request, long subscription) {
        this.request = request;
        this.subscription = subscription;
    }

    public static Unsubscribe parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "UNSUBSCRIBE", 3);
        return new Unsubscribe(MessageUtil.parseLong(wmsg.get(1)), MessageUtil.parseLong(wmsg.get(2)));
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
