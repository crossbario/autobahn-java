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

package io.crossbar.autobahn.demogallery.xbr;

import java.math.BigInteger;
import java.util.List;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleBuyer;
import xbr.network.Util;

public class Buyer {

    private BigInteger mRemainingBalance;
    private Session mSession;
    private SimpleBuyer mBuyer;

    public void buy() {
        mSession = new Session();
        mSession.addOnJoinListener(this::onJoin);
        Client client = new Client(mSession, "ws://10.0.2.2:8080/ws", "realm1");
        client.connect().whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");
        mBuyer = new SimpleBuyer(
                "0x3e5e9111ae8eb78fe1cc3bb8915d5d461f3ef9a9",
                "395df67f0c2d2d9fe1ad08d1bc8b6627011959b79c53d7dd6a3536a33ab8a4fd",
                Util.toXBR(50)
        );
        mBuyer.start(session, details.authid).whenComplete((balance, throwable) -> {
            mRemainingBalance = balance;
            mSession.subscribe("io.crossbar.example", this::actuallyBuy);
        });
    }

    private void actuallyBuy(List<Object> objects) {
        byte[] keyID = (byte[]) objects.get(0);
        String serializer = (String) objects.get(1);
        byte[] ciphertext = (byte[]) objects.get(2);
        mBuyer.unwrap(keyID, serializer, ciphertext).whenComplete((s, throwable) -> {
            System.out.println(s);
        });
    }
}
