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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

public class CBORSerializer implements ISerializer {

    private ObjectMapper mMapper;
    private CBORFactory mFactory;

    public CBORSerializer() {
        mMapper = new ObjectMapper(new CBORFactory());
        mFactory = new CBORFactory();
        mFactory.setCodec(mMapper);
    }

    @Override
    public byte[] serialize(List<Object> message) throws Exception {
        return mMapper.writeValueAsBytes(message);
    }

    @Override
    public List<Object> unserialize(byte[] payload, boolean isBinary) throws Exception {
        return mMapper.readValue(payload, List.class);
    }

    @Override
    public <T> T unserialize(byte[] payload, boolean isBinary, Class<?> collectionClass, Class<?>... subclasses)
            throws Exception {
        JavaType type = mMapper.getTypeFactory().constructParametricType(collectionClass, subclasses);
        return mMapper.readValue(payload, type);
    }

    @Override
    public JsonParser getParser(byte[] rawMessage) throws Exception {
        return mFactory.createParser(rawMessage);
    }

    @Override
    public ObjectMapper getMapper() {
        return mMapper;
    }
}
