package io.crossbar.autobahn.wamp.types;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Message implements IMessage {
    @Override
    public byte[] serialize(Serializer serializer) {
        return new byte[0];
    }

    @Override
    public void uncache() {

    }

    @Override
    public Message parse(List<Object> wmsg) {
        return null;
    }
}
