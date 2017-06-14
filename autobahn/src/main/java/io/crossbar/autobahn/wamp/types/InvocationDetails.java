package io.crossbar.autobahn.wamp.types;

public class InvocationDetails {

    // The registration on which this invocation is for.
    public final Registration mRegistration;

    // The URI of the procedure invoked under the registration.
    public final String mProcedure;

    // The WAMP sessionid of the caller.
    public final long mCallerSessionID;

    // The WAMP authid of the caller.
    public final String mCallerAuthID;

    // The WAMP authrole of the caller.
    public final String mCallerAuthRole;

    // FIXME
    // we need a progress() callback here to allow
    // the user to produce progressive results.

    // XXXX - Tentative, the constructor parameter order may change.
    public InvocationDetails(Registration registration, String procedure, long callerSessionID,
                             String callerAuthID, String callerAuthRole) {
        mRegistration = registration;
        mProcedure = procedure;
        mCallerSessionID = callerSessionID;
        mCallerAuthID = callerAuthID;
        mCallerAuthRole = callerAuthRole;
    }
}
