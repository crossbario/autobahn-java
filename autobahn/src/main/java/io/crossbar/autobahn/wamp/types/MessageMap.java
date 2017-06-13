package io.crossbar.autobahn.wamp.types;

import java.util.HashMap;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class MessageMap {
    public static final Map<Integer, Class<? extends IMessage>> MESSAGE_TYPE_MAP = new HashMap();

    static {
        MESSAGE_TYPE_MAP.put(Hello.MESSAGE_TYPE, Hello.class);
        MESSAGE_TYPE_MAP.put(Welcome.MESSAGE_TYPE, Welcome.class);
    }
}
