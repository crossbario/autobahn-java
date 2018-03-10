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

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Call implements IMessage {

    public static final int MESSAGE_TYPE = 48;

    private static final int TIMEOUT_DEFAULT = 0;

    public final long request;
    public final String procedure;
    public final List<Object> args;
    public final Map<String, Object> kwargs;
    public final int timeout;

    public Call(long request, String procedure, List<Object> args, Map<String, Object> kwargs, int timeout) {
        this.request = request;
        this.procedure = procedure;
        this.args = args;
        this.kwargs = kwargs;
        if (timeout < 1) {
            this.timeout = TIMEOUT_DEFAULT;
        } else {
            this.timeout = timeout;
        }
    }

    public static Call parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "CALL", 4, 6);

        long request = MessageUtil.parseLong(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String procedure = (String) wmsg.get(3);

        List<Object> args = null;
        if (wmsg.size() > 4) {
            if (wmsg.get(4) instanceof byte[]) {
                throw new ProtocolError("Binary payload not supported");
            }
            args = (List<Object>) wmsg.get(4);
        }

        Map<String, Object> kwargs = null;
        if (wmsg.size() > 5) {
            kwargs = (Map<String, Object>) wmsg.get(5);
        }

        int timeout = getOrDefault(options, "timeout", TIMEOUT_DEFAULT);

        return new Call(request, procedure, args, kwargs, timeout);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> options = new HashMap<>();
        if (timeout > TIMEOUT_DEFAULT) {
            options.put("timeout", timeout);
        }
        marshaled.add(options);
        marshaled.add(procedure);
        if (kwargs != null) {
            if (args == null) {
                // Empty args.
                marshaled.add(Collections.emptyList());
            } else {
                marshaled.add(args);
            }
            marshaled.add(kwargs);
        } else if (args != null) {
            marshaled.add(args);
        }
        return marshaled;
    }
}
