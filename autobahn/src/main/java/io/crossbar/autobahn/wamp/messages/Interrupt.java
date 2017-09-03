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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Interrupt implements IMessage {

    public static final int MESSAGE_TYPE = 69;

    private static final String ABORT = "abort";
    private static final String KILL = "kill";

    public final long request;
    public final String mode;

    public Interrupt(long request, String mode) {
        this.request = request;
        this.mode = mode;
    }

    public static Interrupt parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "INTERRUPT", 3);

        long request = MessageUtil.parseRequestID(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String mode = (String) options.getOrDefault("mode", null);
        if (mode != null) {
            if (!Objects.equals(mode, ABORT) && !Objects.equals(mode, KILL)) {
                throw new ProtocolError(String.format("invalid value %s for 'mode' option in INTERRUPT", mode));
            }
        }
        return new Interrupt(request, mode);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (mode != null) {
            Map<String, Object> options = new HashMap<>();
            options.put("mode", mode);
            marshaled.add(options);
        } else {
            // Empty options as third item.
            marshaled.add(Collections.emptyMap());
        }
        return marshaled;
    }
}
