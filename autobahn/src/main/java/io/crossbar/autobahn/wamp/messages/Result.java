package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.Cast;

public class Result implements IMessage {
    public static final int MESSAGE_TYPE = 50;

    public final long request;
    public final List<Object> args;
    public final Map<String, Object> kwargs;

    public Result(long request, List<Object> args, Map<String, Object> kwargs) {
        this.request = request;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Result parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() < 4 || wmsg.size() > 6) {
            throw new ProtocolError(String.format("invalid message length %s for RESULT", wmsg.size()));
        }

        long request = Cast.castRequestID(wmsg.get(1));
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
        return new Result(request, args, kwargs);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        marshaled.add(new HashMap<>());
        if (kwargs != null) {
            if (args == null) {
                // Empty args.
                marshaled.add(new ArrayList<String>());
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
