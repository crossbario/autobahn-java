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

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;


public class CBORSerializer extends ISerializer {

    public CBORSerializer() {
        super(new CBORFactory());
    }
}
