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

package io.crossbar.autobahn.wamp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class Client {

    private final List<ITransport> mTransports;

    private Session mSession;
    private String mRealm;
    private List<IAuthenticator> mAuthenticators;

    private ExecutorService mExecutor;

    public Client(List<ITransport> transports) {
        mTransports = transports;
    }

    public Client(List<ITransport> transports, ExecutorService executor) {
        this(transports);
        mExecutor = executor;
    }

    private ExecutorService getExecutor() {
        if (mExecutor == null) {
            mExecutor = ForkJoinPool.commonPool();
        }
        return mExecutor;
    }

    public void add(Session session, String realm, List<IAuthenticator> authenticators) {
        if (mSession != null) {
            throw new IllegalStateException("Addition of multiple sessions not implemented");
        }
        mSession = session;
        mRealm = realm;
        mAuthenticators = authenticators;
    }

    public CompletableFuture<ExitInfo> connect() {
        CompletableFuture<ExitInfo> exitFuture = new CompletableFuture<>();
        mSession.addOnConnectListener((session) -> mSession.join(mRealm, null));
        mSession.addOnDisconnectListener((session, wasClean) -> exitFuture.complete(new ExitInfo(wasClean)));
        // FIXME: Add error handling but it probably depends on refactoring of the transport.
        CompletableFuture.runAsync(() -> mTransports.get(0).connect(mSession), getExecutor());
        return exitFuture;
    }
}
