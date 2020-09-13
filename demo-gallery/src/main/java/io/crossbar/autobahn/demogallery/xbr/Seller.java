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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleSeller;
import xbr.network.Util;
import xbr.network.eip712.MarketMemberLogin;

public class Seller {

    private static final String TAG = Seller.class.getName();
    private static final String DELEGATE_ETH_KEY = "d99b5b29e6da2528bf458b26237a6cf8655a3e3276c1cdc0de1f98cefee81c01";
    private static final String MEMBER_ETH_KEY = "2eac15546def97adc6d69ca6e28eec831189baa2533e7910755d15403a0749e8";
    private static final String CS_KEY = "0db085a389c1216ad62b88b408e1d830abca9c9f9dad67eb8c8f8734fe7575eb";

    private SimpleSeller mSeller;
    private String mURI;
    private String mRealm;

    public Seller(String uri, String realm) {
        mURI = uri;
        mRealm = realm;
    }

    public void sell() {
        Session session = new Session();
        session.addOnJoinListener(this::onJoin);

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
            Client client = new Client(session, mURI, mRealm, auth);

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

        byte[] apiID = new byte[16];
        String topic = "xbr.myapp.example";
        AtomicReference<BigInteger> balance = new AtomicReference<>();

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {
            String marketMaker = (String) config.get("marketmaker");
            mSeller = new SimpleSeller(marketMaker, DELEGATE_ETH_KEY);
            BigInteger price = Util.toXBR(1);
            int intervalSeconds = 10;
            mSeller.add(apiID, topic, price, intervalSeconds);
            return mSeller.start(session);
        }).thenCompose(bigInteger -> {
            balance.set(bigInteger);
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "crossbario");
            payload.put("country", "DE");
            payload.put("level", "Crossbar is super cool!");
            return mSeller.wrap(apiID, topic, payload);
        }).thenCompose(enc -> {
            return session.publish(topic, new PublishOptions(true, true), enc.get("id"),
                    enc.get("serializer"), enc.get("ciphertext"));
        }).thenAccept(publication -> {
            System.out.println("BALANCE IS " + Util.toInt(balance.get()) + " XBR");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }
}
