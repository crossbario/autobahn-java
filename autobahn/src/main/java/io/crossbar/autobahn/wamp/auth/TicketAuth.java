package io.crossbar.autobahn.wamp.auth;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.Session;


public class TicketAuth implements IAuthenticator {

    public final String authmethod = "ticket";
    public final String authid;
    public final String ticket;

    public TicketAuth (String authid, String ticket) {
        this.authid = authid;
        this.ticket = ticket;
    }

    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        return CompletableFuture.completedFuture(this.ticket);
    }
}
