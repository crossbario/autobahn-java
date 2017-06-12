package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.types.Message;

public interface ITransportHandler {

    // all of the following methods need to be implemented in Session

    void onConnect(ITransport transport);

    void onMessage(Message message);

    void onDisconnect(boolean wasClean);

    boolean isConnected();
}
