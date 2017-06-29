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

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;


public interface ISerializer {

    byte[] serialize(List<Object> message);

    List<Object> unserialize(byte[] payload, boolean isBinary);

    <T> T convertValue(Object fromValue, TypeReference toValueTypeRef);
}
