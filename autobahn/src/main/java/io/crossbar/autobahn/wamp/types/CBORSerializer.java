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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

public class CBORSerializer implements ISerializer {

    private ObjectMapper mMapper;

    public CBORSerializer() {
        mMapper = new ObjectMapper(new CBORFactory());
    }

    @Override
    public byte[] serialize(List<Object> message) throws Exception {
        return mMapper.writeValueAsBytes(message);
    }

    @Override
    public List<Object> unserialize(byte[] payload, boolean isBinary) throws Exception {
        return mMapper.readValue(payload, new TypeReference<List<Object>>() {});
    }

    @Override
    public ObjectMapper getMapper() {
        return mMapper;
    }
}
