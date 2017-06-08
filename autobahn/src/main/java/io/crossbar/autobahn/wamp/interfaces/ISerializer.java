package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.types.Message;

public interface ISerializer {

    Map<byte[], Boolean> serialize(Message message);

    List<Message> unserialize(byte[] payload, boolean isBinary);

}
