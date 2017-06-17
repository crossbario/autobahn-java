package io.crossbar.autobahn.wamp.interfaces;

// FIXME: data types to be discussed/changed.
public interface ITransport {

    // this is the only method needed by Session ..
    void send(IMessage message);

    // .. not sure about the following, we'll see
    void connect(ITransportHandler transportHandler);

    boolean isOpen();

    void close();

    void abort();
}
