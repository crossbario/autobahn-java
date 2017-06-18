import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;

public class TicketAuth implements IAuthenticator {

    public final String authmethod = "ticket";
    public final String authid;
    public final String ticket;

    public TicketAuth (String authid, String ticket) {
        this.authid = authid;
        this.ticket = ticket;
    }

    CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        return CompletableFuture.completedFuture(this.ticket);
    }
}
