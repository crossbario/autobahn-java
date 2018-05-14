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

import com.fasterxml.jackson.core.JsonFactory;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;


public class JSONSerializer extends ISerializer {

    public static final String NAME = "wamp.2.json";
    public static final int RAWSOCKET_SERIALIZER_ID = 1;

    public JSONSerializer() {
        super(new JsonFactory());
    }

    @Override
    public boolean isBinary() {
        return false;
    }
}
