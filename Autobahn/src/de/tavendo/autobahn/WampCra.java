package de.tavendo.autobahn;

import de.tavendo.autobahn.Wamp.CallHandler;

public interface WampCra extends Wamp {
    
    /**
     * Auth handler.
     */
    public interface AuthHandler {

       /**
        * Fired on successful completion of authentication.
        *
        * @param permissions    The permissions obtained on successful authentication.
        */
       public void onAuthSuccess(Object permissions);

       /**
        * Fired on authentication failure.
        *
        * @param errorUri   The URI or CURIE of the error that occurred.
        * @param errorDesc  A human readable description of the error.
        */
       public void onAuthError(String errorUri, String errorDesc);
    }
    
    
    /**
     * Call a remote procedure (RPC).
     *
     * @param authKey       The URI or CURIE of the remote procedure to call.
     * @param authSecret    The type the call result gets transformed into.
     * @param callHandler   The handler to be invoked upon call completion.
     * @param arguments     Zero, one or more arguments for the call.
     */
    public void authenticate(AuthHandler authHandler, String authKey, String authSecret, Object... authExtra);
    
    
}
