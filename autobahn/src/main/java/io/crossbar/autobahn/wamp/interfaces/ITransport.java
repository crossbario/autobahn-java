package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.types.Message;

// FIXME: data types to be discussed/changed.
public interface ITransport {

    void send(Message message);

    boolean isOpen();

    void close();

    void abort();

    int getChannelID();
}
