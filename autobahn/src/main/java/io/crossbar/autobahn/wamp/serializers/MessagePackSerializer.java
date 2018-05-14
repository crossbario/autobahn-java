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

import org.msgpack.jackson.dataformat.MessagePackFactory;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;


public class MessagePackSerializer extends ISerializer {

    public static final String NAME = "wamp.2.msgpack";
    public static final int RAWSOCKET_SERIALIZER_ID = 2;

    public MessagePackSerializer() {
        super(new MessagePackFactory());
    }
}
