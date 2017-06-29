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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public interface ISerializer {

    byte[] serialize(List<Object> message) throws Exception;

    List<Object> unserialize(byte[] payload, boolean isBinary) throws Exception;

    <T> T unserialize(byte[] payload, boolean isBinary, Class<?> collectionClass, Class<?>... subclasses)
            throws Exception;

    JsonParser getParser(byte[] rawMessage) throws Exception;

    ObjectMapper getMapper();

}
