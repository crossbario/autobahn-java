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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class Client {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private final List<ITransport> mTransports;

    private Session mSession;
    private String mRealm;
    private List<IAuthenticator> mAuthenticators;

    private ExecutorService mExecutor;

    public Client(String webSocketURL) {
        mTransports = new ArrayList<>();
        try {
            mTransports.add(selectTransport(webSocketURL));
        } catch (Exception e) {
            // We don't really expect to be here. Its most likely
            // that we changed our WAMP transports names.
            // The other reason could be the change of our transport'
            // constructor.
            e.printStackTrace();
        }
    }

    public Client(ITransport transport) {
        mTransports = new ArrayList<>();
        mTransports.add(transport);
    }

    public Client(ITransport transport, ExecutorService executor) {
        this(transport);
        mExecutor = executor;
    }

    public Client(String webSocketURL, ExecutorService executor) {
        this(webSocketURL);
        mExecutor = executor;
    }

    public Client(Session session, String webSocketURL, String realm) {
        this(webSocketURL);
        mSession = session;
        mRealm = realm;
    }

    public Client(Session session, String webSocketURL, String realm, ExecutorService executor) {
        this(webSocketURL);
        mSession = session;
        mRealm = realm;
        mExecutor = executor;
    }

    public Client(List<ITransport> transports) {
        mTransports = transports;
    }

    public Client(List<ITransport> transports, ExecutorService executor) {
        this(transports);
        mExecutor = executor;
    }

    private ITransport selectTransport(String webSocketURL) throws Exception {
        Class<?> transportClass;
        if (System.getProperty("java.vendor").equals("The Android Project")) {
            transportClass = Class.forName("io.crossbar.autobahn.wamp.transports.AndroidWebSocket");
        } else {
            transportClass = Class.forName("io.crossbar.autobahn.wamp.transports.NettyTransport");
        }
        return (ITransport) transportClass.getConstructor(String.class).newInstance(webSocketURL);
    }

    private ExecutorService getExecutor() {
        return mExecutor == null ? ForkJoinPool.commonPool(): mExecutor;
    }

    public void add(Session session, String realm, List<IAuthenticator> authenticators) {
        if (mSession != null) {
            throw new IllegalStateException("Addition of multiple sessions not implemented");
        }
        mSession = session;
        mRealm = realm;
        mAuthenticators = authenticators;
    }

    public void add(Session session, String realm) {
        add(session, realm, null);
    }

    public CompletableFuture<ExitInfo> connect() {
        CompletableFuture<ExitInfo> exitFuture = new CompletableFuture<>();
        mSession.addOnConnectListener((session) ->
                mSession.join(mRealm, null).thenAccept(details ->
                        LOGGER.info(String.format("JOINED session=%s realm=%s", details.sessionID,
                                details.realm))));
        mSession.addOnDisconnectListener((session, wasClean) -> exitFuture.complete(new ExitInfo(wasClean)));
        CompletableFuture.runAsync(() -> {
            try {
                mTransports.get(0).connect(mSession);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, getExecutor());
        return exitFuture;
    }
}
