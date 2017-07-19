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

package io.crossbar.autobahn.wamp.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

public class MessagePackSerializer implements ISerializer {

    public final ObjectMapper mMapper;

    public MessagePackSerializer() {
        mMapper = new ObjectMapper(new MessagePackFactory());
    }

    @Override
    public byte[] serialize(List<Object> message) {
        try {
            return mMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Object> unserialize(byte[] payload, boolean isBinary) {
        try {
            return mMapper.readValue(payload, new TypeReference<List<Object>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeReference toValueTypeRef) {
        return mMapper.convertValue(fromValue, toValueTypeRef);
    }
}
