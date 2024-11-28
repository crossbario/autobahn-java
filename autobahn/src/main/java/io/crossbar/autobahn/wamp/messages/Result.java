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
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

public class Result implements IMessage {
    public static final int MESSAGE_TYPE = 50;

    public final long request;
    public final List<Object> args;
    public final Map<String, Object> kwargs;
    public final Map<String, Object> options;

    public Result(long request, List<Object> args, Map<String, Object> kwargs, Map<String, Object> options) {
        this.request = request;
        this.args = args;
        this.kwargs = kwargs;
        this.options = options;
    }

    public static Result parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "RESULT", 3, 5);

        long request = MessageUtil.parseLong(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        List<Object> args = null;
        if (wmsg.size() > 3) {
            if (wmsg.get(3) instanceof byte[]) {
                throw new ProtocolError("Binary payload not supported");
            }
            args = (List<Object>) wmsg.get(3);
        }
        Map<String, Object> kwargs = null;
        if (wmsg.size() > 4) {
            kwargs = (Map<String, Object>) wmsg.get(4);
        }
        return new Result(request, args, kwargs, options);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        if (options == null) {
            marshaled.add(Collections.emptyMap());
        } else {
            marshaled.add(options);
        }
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
