package io.crossbar.autobahn.wamp.types;

public class Registration {
    public final long registration;
    public final String procedure;
    public final IInvocationHandler endpoint;

    public Registration(long registration, String procedure, IInvocationHandler endpoint) {
        this.registration = registration;
        this.procedure = procedure;
        this.endpoint = endpoint;
    }
}
