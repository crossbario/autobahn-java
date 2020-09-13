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

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleBuyer;
import xbr.network.Util;
import xbr.network.eip712.MarketMemberLogin;

public class Buyer {

    private static final String DELEGATE_ETH_KEY = "77c5495fbb039eed474fc940f29955ed0531693cc9212911efd35dff0373153f";
    private static final String MEMBER_ETH_KEY = "2e114163041d2fb8d45f9251db259a68ee6bdbfd6d10fe1ae87c5c4bcd6ba491";
    private static final String CS_KEY = "dc88492fcff5470fcc76f21fa03f1752e0738e1e5cd56cd61fc280bac4d4c4d9";

    private BigInteger mRemainingBalance;
    private Session mSession;
    private SimpleBuyer mBuyer;

    public void buy() {
        mSession = new Session();
        mSession.addOnJoinListener(this::onJoin);

        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(MEMBER_ETH_KEY));
        String addressHex = Credentials.create(keyPair).getAddress();
        byte[] addressRaw = Numeric.hexStringToByteArray(addressHex);

        String pubkeyHex = CryptosignAuth.getPublicKey(AuthUtil.toBinary(CS_KEY));

        Map<String, Object> extras = new HashMap<>();
        extras.put("wallet_address", addressRaw);
        extras.put("pubkey", pubkeyHex);

        MarketMemberLogin.sign(
                keyPair, addressHex, pubkeyHex
        ).thenCompose(signature -> {
            extras.put("signature", signature);

            CryptosignAuth auth = new CryptosignAuth("public", CS_KEY, extras);
            Client client = new Client(mSession, "ws://10.0.2.2:8070/ws", "idma", auth);

            return client.connect();
        }).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");

        System.out.println(details.authrole);

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {
            System.out.println("STASRTINGG..");
            String marketMaker = (String) config.get("marketmaker");
            mBuyer = new SimpleBuyer(marketMaker, DELEGATE_ETH_KEY, Util.toXBR(50));
            return mBuyer.start(session, details.authid);
        }).thenAccept(balance -> {
            mRemainingBalance = balance;
            mSession.subscribe("xbr.myapp.example", this::actuallyBuy);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
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
