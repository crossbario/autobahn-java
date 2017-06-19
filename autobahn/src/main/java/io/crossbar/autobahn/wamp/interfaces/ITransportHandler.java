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

    // all of the following methods need to be implemented in Session

    void onConnect(ITransport transport);

    void onMessage(IMessage message);

    void onDisconnect(boolean wasClean);

    boolean isConnected();
}
