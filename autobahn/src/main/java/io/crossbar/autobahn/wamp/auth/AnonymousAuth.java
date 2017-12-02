///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.auth;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.Session;

public class AnonymousAuth implements IAuthenticator {

    public static final String authmethod = "anonymous";
    public final String authid = null;

    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        throw new UnsupportedOperationException("Anonymous auth does not support challenge.");
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
