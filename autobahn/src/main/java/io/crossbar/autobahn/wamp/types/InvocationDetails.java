package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;


public class InvocationDetails {

    // The registration on which this invocation is for.
    public final Registration registration;

    // The URI of the procedure invoked under the registration.
    public final String procedure;

    // The WAMP sessionid of the caller.
    public final long caller_sessionid;

    // The WAMP authid of the caller.
    public final String caller_authid;

    // The WAMP authrole of the caller.
    public final String caller_authrole;

    // FIXME
    // we need a progress() callback here to allow
    // the user to produce progressive results.
}
