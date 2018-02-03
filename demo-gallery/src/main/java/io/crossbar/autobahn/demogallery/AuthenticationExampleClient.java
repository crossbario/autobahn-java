package io.crossbar.autobahn.demogallery;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.ChallengeResponseAuth;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.auth.TicketAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class AuthenticationExampleClient {

    private static CompletableFuture<ExitInfo> connect(
            String websocketURL, String realm, IAuthenticator authenticator) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> System.out.println("Joined session."));
        Client client = new Client(wampSession, websocketURL, realm, authenticator);
        return client.connect();
    }

    public static CompletableFuture<ExitInfo> exampleTicketAuth(
            String websocketURL, String realm, String authid, String ticket) {
        return connect(websocketURL, realm, new TicketAuth(authid, ticket));
    }

    public static CompletableFuture<ExitInfo> exampleCRA(
            String websocketURL, String realm, String authid, String secret) {
        return connect(websocketURL, realm, new ChallengeResponseAuth(authid, secret));
    }

    public static CompletableFuture<ExitInfo> exampleCryptoSign(
            String websocketURL, String realm, String authid, String privkey, String pubkey) {
        return connect(websocketURL, realm, new CryptosignAuth(authid, privkey, pubkey));
    }
}
