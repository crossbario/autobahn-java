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

import java.util.HashMap;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class MessageMap {
    public static final Map<Integer, Class<? extends IMessage>> MESSAGE_TYPE_MAP = new HashMap();

    static {
        MESSAGE_TYPE_MAP.put(Hello.MESSAGE_TYPE, Hello.class);
        MESSAGE_TYPE_MAP.put(Challenge.MESSAGE_TYPE, Challenge.class);
        MESSAGE_TYPE_MAP.put(Welcome.MESSAGE_TYPE, Welcome.class);
        MESSAGE_TYPE_MAP.put(Abort.MESSAGE_TYPE, Abort.class);
        MESSAGE_TYPE_MAP.put(Goodbye.MESSAGE_TYPE, Goodbye.class);
        MESSAGE_TYPE_MAP.put(Error.MESSAGE_TYPE, Error.class);
        MESSAGE_TYPE_MAP.put(Publish.MESSAGE_TYPE, Publish.class);
        MESSAGE_TYPE_MAP.put(Published.MESSAGE_TYPE, Published.class);
        MESSAGE_TYPE_MAP.put(Subscribe.MESSAGE_TYPE, Subscribe.class);
        MESSAGE_TYPE_MAP.put(Subscribed.MESSAGE_TYPE, Subscribed.class);
        MESSAGE_TYPE_MAP.put(Unsubscribe.MESSAGE_TYPE, Unsubscribe.class);
        MESSAGE_TYPE_MAP.put(Unsubscribed.MESSAGE_TYPE, Unsubscribed.class);
        MESSAGE_TYPE_MAP.put(Event.MESSAGE_TYPE, Event.class);
        MESSAGE_TYPE_MAP.put(Call.MESSAGE_TYPE, Call.class);
        MESSAGE_TYPE_MAP.put(Result.MESSAGE_TYPE, Result.class);
        MESSAGE_TYPE_MAP.put(Register.MESSAGE_TYPE, Register.class);
        MESSAGE_TYPE_MAP.put(Registered.MESSAGE_TYPE, Registered.class);
        MESSAGE_TYPE_MAP.put(Unregister.MESSAGE_TYPE, Unregister.class);
        MESSAGE_TYPE_MAP.put(Unregistered.MESSAGE_TYPE, Unregistered.class);
        MESSAGE_TYPE_MAP.put(Invocation.MESSAGE_TYPE, Invocation.class);
        MESSAGE_TYPE_MAP.put(Yield.MESSAGE_TYPE, Yield.class);
        MESSAGE_TYPE_MAP.put(Interrupt.MESSAGE_TYPE, Interrupt.class);
    }
}
