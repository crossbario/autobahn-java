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

public class Error implements IMessage {

    public static final int MESSAGE_TYPE = 8;

    public final int requestType;
    public final long request;
    public final String error;
    public final List<Object> args;
    public final Map<String, Object> kwargs;

    public Error(int requestType, long request, String error, List<Object> args, Map<String, Object> kwargs) {
        this.requestType = requestType;
        this.request = request;
        this.error = error;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Error parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "ERROR", 5, 7);

        int requestType = (int) wmsg.get(1);
        // FIXME: add validation here. see:
        // https://github.com/crossbario/autobahn-python/blob/886973ca139916176d5db707ffc7fa20f9529010/autobahn/wamp/message.py#L1291
        long request = MessageUtil.parseLong(wmsg.get(2));
        Map<String, Object> details = (Map<String, Object>) wmsg.get(3);
        String error = (String) wmsg.get(4);

        if (wmsg.size() == 6 && wmsg.get(5) instanceof byte[]) {
            throw new ProtocolError("Binary payload not supported");
        }

        List<Object> args = null;
        if (wmsg.size() > 5) {
            args = (List<Object>) wmsg.get(5);
        }

        Map<String, Object> kwargs = null;
        if (wmsg.size() > 6) {
            kwargs = (Map<String, Object>) wmsg.get(6);
        }

        return new Error(requestType, request, error, args, kwargs);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(requestType);
        marshaled.add(request);
        marshaled.add(new HashMap<String, Object>());
        marshaled.add(error);
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
