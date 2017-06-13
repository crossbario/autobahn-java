package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;

public interface ISerializer {

    byte[] serialize(List<Object> message);

    List<Object> unserialize(byte[] payload, boolean isBinary);

}
