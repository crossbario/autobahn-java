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


public interface ITransportHandler {

    void onConnect(ITransport transport, ISerializer serializer) throws Exception;

    void onMessage(byte[] payload, boolean isBinary) throws Exception;

    void onDisconnect(boolean wasClean);

    boolean isConnected();
}
