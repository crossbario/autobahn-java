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

import io.crossbar.autobahn.wamp.interfaces.ISerializer;


public interface ITransport {

    void send(byte[] payload, boolean isBinary);

    void connect(ITransportHandler transportHandler);

    boolean isOpen();

    void close();

    void abort();
}
