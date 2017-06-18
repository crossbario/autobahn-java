package io.crossbar.autobahn.wamp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class Client {
    private final Session mSession;
    private final List<ITransport> mTransports;
    private final String mRealm;
    private final List<?> mAuthenticators;

    public Client(Session session, List<ITransport> transports, String realm,
                  List<?> authenticators) {
        mSession = session;
        mTransports = transports;
        mRealm = realm;
        mAuthenticators = authenticators;
    }

    public CompletableFuture<ExitInfo> connect() {
        CompletableFuture<ExitInfo> exitFuture = new CompletableFuture<>();
        ExitInfo info = new ExitInfo();
        mSession.addOnConnectListener(() -> mSession.join(mRealm, null));
        mSession.addOnDisconnectListener(() -> exitFuture.complete(info));
        mTransports.get(0).connect(mSession);
        return exitFuture;
    }
}
