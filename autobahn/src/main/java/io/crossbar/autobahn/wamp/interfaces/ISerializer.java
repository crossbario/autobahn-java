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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;


public abstract class ISerializer {

    private static final IABLogger LOGGER = ABLogger.getLogger(ISerializer.class.getName());

    public final ObjectMapper mapper;

    public ISerializer(JsonFactory factor) {
        mapper = new ObjectMapper(factor);
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ISerializer(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    public byte[] serialize(List<Object> message) {
        try {
            return mapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            LOGGER.v(e.getMessage(), e);
            return null;
        }
    }

    public List<Object> unserialize(byte[] payload, boolean isBinary) {
        try {
            return mapper.readValue(payload, new TypeReference<List<Object>>() {});
        } catch (IOException e) {
            LOGGER.v(e.getMessage(), e);
            return null;
        }
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        // https://github.com/FasterXML/jackson-databind#tutorial-fancier-stuff-conversions
        // ResultType result = mapper.convertValue(sourceObject, ResultType.class);
        return mapper.convertValue(fromValue, toValueTypeRef);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueTypeClass) {
        // https://github.com/FasterXML/jackson-databind#tutorial-fancier-stuff-conversions
        // ResultType result = mapper.convertValue(sourceObject, ResultType.class);
        return mapper.convertValue(fromValue, toValueTypeClass);
    }

    public boolean isBinary() {
        return true;
    }
}
