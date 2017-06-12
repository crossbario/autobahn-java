package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;

import io.crossbar.autobahn.wamp.types.Message;

// FIXME: data types to be discussed/changed.
public interface ITransport {

    void connect(String url, List<String> subProtocols, ITransportHandler transportHandler);

    void send(Message message);

    boolean isOpen();

    void close();

    void abort();
}
