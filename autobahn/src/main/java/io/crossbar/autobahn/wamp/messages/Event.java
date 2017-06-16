package io.crossbar.autobahn.wamp.messages;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Event implements IMessage {

    public static final int MESSAGE_TYPE = 36;

    private final long subscription;
    private final long publication;
    private final List<Object> args;
    private final Map<String, Object> kwargs;

    public Event(long subscription, long publication, List<Object> args, Map<String, Object> kwargs) {
        this.subscription = subscription;
        this.publication = publication;
        this.args = args;
        this.kwargs = kwargs;
    }

    public static Event parse(List<Object> wmsg) {
        return null;
    }

    @Override
    public List<Object> marshal() {
        return null;
    }
}
