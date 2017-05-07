/******************************************************************************
 *
 *  Copyright 2012 Alejandro Hernandez
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

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
     */
    public void authenticate(AuthHandler authHandler, String authKey, String authSecret);
    
    /**
     * Authenticate the WAMP Session.
     *
     * @param authHandler   The handler to be invoked upon authentication completion.
     * @param authKey       The user Key for authentication.
     * @param authSecret    The user Secret for authentication.
     * @param authExtra     Zero, one or more extra arguments for the authentication.
     */
    public void authenticate(AuthHandler authHandler, String authKey, String authSecret, Object authExtra);
    
    
}
