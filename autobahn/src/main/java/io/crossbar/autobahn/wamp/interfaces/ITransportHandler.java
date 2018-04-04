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


import io.crossbar.autobahn.wamp.types.CloseDetails;

public interface ITransportHandler {

    void onConnect(ITransport transport, ISerializer serializer) throws Exception;

    void onMessage(byte[] payload, boolean isBinary) throws Exception;

    void onLeave(CloseDetails details);

    void onDisconnect(boolean wasClean);

    boolean isConnected();
}
