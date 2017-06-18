package io.crossbar.autobahn.wamp.auth;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.Session;

public class AnonymousAuth implements IAuthenticator {

    public final String authmethod = "anonymous";
    public final String authid = null;

    public AnonymousAuth () {
    }

    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        // anonymous authentication in WAMP will NOT invoke this callback!
    }
}
