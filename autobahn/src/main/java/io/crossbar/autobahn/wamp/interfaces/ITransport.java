package io.crossbar.autobahn.wamp.interfaces;

// FIXME: data types to be discussed/changed.
public interface ITransport {

    void send();

    boolean isOpen();

    void close();

    void abort();

    int getChannelID();
}
