package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;

import io.crossbar.autobahn.wamp.types.Message;
import io.crossbar.autobahn.wamp.types.Serializer;

public interface IMessage {

    byte[] serialize(Serializer serializer);

    Message parse(List<Object> wmsg);

}
