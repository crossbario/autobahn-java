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

package io.crossbar.autobahn.wamp.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.io.IOException;
import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

public class CBORSerializer implements ISerializer {

    private ObjectMapper mMapper;

    public CBORSerializer() {
        mMapper = new ObjectMapper(new CBORFactory());
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
            return mMapper.readValue(payload, List.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> T unserialize(byte[] payload, boolean isBinary, Class<T> Klass) {
        try {
            return mMapper.readValue(payload, Klass);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
