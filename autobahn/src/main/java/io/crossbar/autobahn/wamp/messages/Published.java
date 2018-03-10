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

public class Published implements IMessage {

    public static final int MESSAGE_TYPE = 17;

    public final long request;
    public final long publication;

    public Published(long request, long publication) {
        this.request = request;
        this.publication = publication;
    }

    public static Published parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "PUBLISHED", 3);
        return new Published(MessageUtil.parseLong(wmsg.get(1)), MessageUtil.parseLong(wmsg.get(2)));
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        marshaled.add(publication);
        return marshaled;
    }
}
