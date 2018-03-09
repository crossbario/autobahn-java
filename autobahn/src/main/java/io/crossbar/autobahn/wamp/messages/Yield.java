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

public class Yield implements IMessage {

    public static final int MESSAGE_TYPE = 70;

    public final long request;
    public final List<Object> args;
    public final Map<String, Object> kwargs;

    public Yield(long request, List<Object> args, Map<String, Object> kwargs) {
        this.request = request;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Yield parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "YIELD", 3, 6);

        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        List<Object> args = null;
        if (wmsg.size() > 3) {
            if (wmsg.get(3) instanceof byte[]) {
                throw new ProtocolError("Binary payload not supported");
            }
            args = (List<Object>) wmsg.get(4);
        }
        Map<String, Object> kwargs = null;
        if (wmsg.size() > 4) {
            kwargs = (Map<String, Object>) wmsg.get(4);
        }
        return new Yield(MessageUtil.parseLong(wmsg.get(1)), args, kwargs);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        // Empty options.
        marshaled.add(Collections.emptyMap());
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
