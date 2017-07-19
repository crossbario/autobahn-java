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

package io.crossbar.autobahn.wamp.interfaces;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;


public abstract class ISerializer {

    private final ObjectMapper mMapper;

    public ISerializer(JsonFactory factor) {
        mMapper = new ObjectMapper(factor);
    }

    public byte[] serialize(List<Object> message) {
        try {
            return mMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Object> unserialize(byte[] payload, boolean isBinary) {
        try {
            return mMapper.readValue(payload, new TypeReference<List<Object>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T convertValue(Object fromValue, TypeReference toValueTypeRef) {
        // https://github.com/FasterXML/jackson-databind#tutorial-fancier-stuff-conversions
        // ResultType result = mapper.convertValue(sourceObject, ResultType.class);
        return mMapper.convertValue(fromValue, toValueTypeRef);
    }
}
