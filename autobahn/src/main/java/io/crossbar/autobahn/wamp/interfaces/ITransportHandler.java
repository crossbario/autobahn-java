package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.types.Message;

public interface ITransportHandler {

    void onOpen(ITransport transport);

    void onMessage(Message message);

    void onClose(boolean wasClean);
}
