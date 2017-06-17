package io.crossbar.autobahn.wamp.types;

public class CallOptions {
    public final int timeout;

    public CallOptions(int timeout) {
        this.timeout = timeout;
    }
}
