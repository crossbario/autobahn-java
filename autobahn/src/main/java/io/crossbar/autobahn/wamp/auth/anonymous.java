import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;

public class AnonymousAuth implements IAuthenticator {

    public final String authmethod = "anonymous";
    public final String authid = null;

    public AnonymousAuth () {
    }

    CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        // anonymous authentication in WAMP will NOT invoke this callback!
    }
}
