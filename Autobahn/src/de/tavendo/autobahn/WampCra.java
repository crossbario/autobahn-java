package de.tavendo.autobahn;


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
     * Authenticate the WAMP Session.
     *
     * @param authHandler   The handler to be invoked upon authentication completion.
     * @param authKey       The user Key for authentication.
     * @param authSecret    The user Secret for authentication.
     * @param authExtra     Zero, one or more extra arguments for the authentication.
     */
    public void authenticate(AuthHandler authHandler, String authKey, String authSecret, Object... authExtra);
    
    
}
