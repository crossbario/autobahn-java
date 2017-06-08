package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.types.EncodedPayload;

public interface IPayloadCodec {

    EncodedPayload encode(boolean isOriginating, String uri, List<Object> args, Map<String, Object> kwargs);

    void decode(boolean isOriginating, String uri, EncodedPayload encodedPayload);
}
