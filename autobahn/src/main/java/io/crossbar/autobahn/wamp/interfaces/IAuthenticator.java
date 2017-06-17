package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.Challenge;

public interface IAuthenticator {
    void onChallenge(Session session, Challenge challenge);
}
