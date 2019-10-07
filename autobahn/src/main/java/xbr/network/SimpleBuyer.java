package xbr.network;


import com.fasterxml.jackson.core.type.TypeReference;

import org.libsodium.jni.SodiumConstants;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;

import static org.libsodium.jni.NaCl.sodium;

public class SimpleBuyer {

    private final String mEthPrivateKey;
    private final String mEthPublicKey;
    private final int mMaxPrice;
    private final byte[] mPrivateKey;
    private final byte[] mPublicKey;
    private final ECKeyPair mECKey;

    private HashMap<String, String> mKeys;
    private Session mSession;
    private boolean mRunning;

    private long mRemainingBalance;
    private HashMap<String, Object> mChannel;

    public SimpleBuyer(String buyerKey, int maxPrice) {
        mECKey = ECKeyPair.create(Numeric.hexStringToByteArray(buyerKey));
        mEthPrivateKey = Numeric.toHexString(mECKey.getPrivateKey().toByteArray());
        mEthPublicKey = Numeric.toHexString(mECKey.getPublicKey().toByteArray());
        mMaxPrice = maxPrice;
        mKeys = new HashMap<>();

        mPrivateKey = new byte[SodiumConstants.SECRETKEY_BYTES];
        mPublicKey = new byte[SodiumConstants.PUBLICKEY_BYTES];
        sodium().crypto_box_keypair(mPublicKey, mPrivateKey);
    }

    public CompletableFuture<Long> start(Session session, long consumerID) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        if (mRunning) {
            throw new IllegalStateException("Already running...");
        }

        mSession = session;
        mRunning = true;

        CompletableFuture<HashMap<String, Object>> callFuture = session.call(
                "xbr.marketmaker.get_payment_channel",
                new TypeReference<HashMap<String, Object>>() {},
                Keys.getAddress(mPublicKey));
        callFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                mChannel = result;
                mRemainingBalance = (int) result.get("remaining");
                future.complete(mRemainingBalance);
            }
        });
        return future;
    }

    public void stop() {
        if (!mRunning) {
            throw new IllegalStateException("Already stopped...");
        }
        mRunning = false;
    }

    public CompletableFuture<HashMap<String, Object>> balance() {
        CompletableFuture<HashMap<String, Object>> future = new CompletableFuture<>();

        if (mSession == null || !mSession.isConnected()) {
            throw new IllegalStateException("Session not connected");
        }

        CompletableFuture<HashMap<String, Object>> callFuture = mSession.call(
                "xbr.marketmaker.get_payment_channel",
                new TypeReference<HashMap<String, Object>>() {},
                Keys.getAddress(mPublicKey));
        callFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                HashMap<String, Object> res = new HashMap<>();
                res.put("amount", result.get("amount"));
                res.put("remaining", result.get("remaining"));
                res.put("inflight", result.get("inflight"));
                future.complete(res);
            }
        });

        return future;
    }

    public CompletableFuture<HashMap<String, Object>> openChannel(byte[] buyerAddr, long amount) {
        CompletableFuture<HashMap<String, Object>> future = new CompletableFuture<>();

        if (mSession == null || !mSession.isConnected()) {
            throw new IllegalStateException("Session not connected");
        }

        CompletableFuture<HashMap<String, Object>> callFuture = mSession.call(
                "xbr.marketmaker.open_payment_channel",
                new TypeReference<HashMap<String, Object>>() {},
                buyerAddr,
                Keys.getAddress(mPublicKey),
                amount,
                new SecureRandom(new byte[64]));
        callFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                HashMap<String, Object> res = new HashMap<>();
                res.put("amount", result.get("amount"));
                res.put("remaining", result.get("remaining"));
                res.put("inflight", result.get("inflight"));
                future.complete(res);
            }
        });


        return future;
    }

    public void closeChannel() {

    }

    public CompletableFuture<String> unwrap(byte[] keyID, String encSerializer, String ciphertext) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (!mKeys.containsKey(keyID)) {
            int amount = mMaxPrice;
            byte[] signature = Hash.sha256(new byte[64]);
            byte[] key = new byte[16];
            sodium().randombytes(key, 16);

            CompletableFuture<HashMap<String, Object>> callFuture = mSession.call(
                    "xbr.marketmaker.buy_key",
                    new TypeReference<HashMap<String, Object>>() {},
                    Keys.getAddress(mPublicKey),
                    mPublicKey,
                    key,
                    amount,
                    signature);
            callFuture.whenComplete((stringObjectHashMap, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                } else {
                    System.out.println(stringObjectHashMap);
                }
            });
        }
        return future;
    }
}
