package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse

public interface IAuthenticator {
    CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge);
}
