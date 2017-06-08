package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

public class Serializer implements ISerializer {
    @Override
    public Map<byte[], Boolean> serialize(Message message) {
        return null;
    }

    @Override
    public List<Message> unserialize(byte[] payload, boolean isBinary) {
        return null;
    }
}
