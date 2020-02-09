package xbr.network;


import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle.jce.ECKeyUtil;
import org.bouncycastle.jce.interfaces.ECKey;
import org.libsodium.jni.SodiumConstants;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;

import static org.libsodium.jni.NaCl.sodium;

public class SimpleBuyer {

    private final byte[] mEthPrivateKey;
    private final byte[] mEthPublicKey;
    private final byte[] mEthAddr;
    private final byte[] mMarketMakerAddr;

    private final BigInteger mMaxPrice;
    private final ECKeyPair mECKey;

    private HashMap<String, String> mKeys;
    private Session mSession;
    private boolean mRunning;

    private BigInteger mRemainingBalance;
    private HashMap<String, Object> mChannel;

    public SimpleBuyer(String marketMakerAddr, String buyerKey, BigInteger maxPrice) {
        mECKey = ECKeyPair.create(Numeric.hexStringToByteArray(buyerKey));
        mMarketMakerAddr = Numeric.hexStringToByteArray(marketMakerAddr);
        mEthPrivateKey = mECKey.getPrivateKey().toByteArray();
        mEthPublicKey = mECKey.getPublicKey().toByteArray();
        mEthAddr = Numeric.hexStringToByteArray(Credentials.create(mECKey).getAddress());

        mMaxPrice = maxPrice;
        mKeys = new HashMap<>();
    }

    private boolean isConnected() {
        return mSession != null && mSession.isConnected();
    }

    public CompletableFuture<BigInteger> start(Session session, String consumerID) {
        CompletableFuture<BigInteger> future = new CompletableFuture<>();
        if (mRunning) {
            future.completeExceptionally(new IllegalStateException("Already running..."));
            return future;
        }

        mSession = session;
        mRunning = true;

        List<Object> args = new ArrayList<>();
        args.add(mEthAddr);
        CompletableFuture<HashMap<String, Object>> payChannelF = mSession.call(
                "xbr.marketmaker.get_active_payment_channel", args,
                new TypeReference<HashMap<String, Object>>() {});
        payChannelF.whenComplete((channel, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                mChannel = channel;
                balance().whenComplete((balance, throwable1) -> {
                    if (throwable1 != null) {
                        future.completeExceptionally(throwable1);
                    } else {
                        future.complete((BigInteger) balance.get("remaining"));
                    }
                });
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> stop() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!mRunning) {
            future.completeExceptionally(new IllegalStateException("Already stopped..."));
            return future;
        }
        mRunning = false;
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<HashMap<String, Object>> balance() {
        CompletableFuture<HashMap<String, Object>> future = new CompletableFuture<>();

        if (!isConnected()) {
            future.completeExceptionally(new IllegalStateException("Session not connected"));
            return future;
        }

        CompletableFuture<HashMap<String, Object>> balanceFuture = mSession.call(
                "xbr.marketmaker.get_payment_channel_balance",
                new TypeReference<HashMap<String, Object>>() {},
                mChannel.get("channel"));
        balanceFuture.whenComplete((paymentBalance, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                mRemainingBalance = new BigInteger((byte[]) paymentBalance.get("remaining"));
                HashMap<String, Object> res = new HashMap<>();
                res.put("amount", paymentBalance.get("amount"));
                res.put("remaining", mRemainingBalance);
                res.put("inflight", paymentBalance.get("inflight"));
                future.complete(res);
            }
        });

        return future;
    }

    public CompletableFuture<HashMap<String, Object>> openChannel(byte[] buyerAddr,
                                                                  BigInteger amount) {
        CompletableFuture<HashMap<String, Object>> future = new CompletableFuture<>();

        if (!isConnected()) {
            future.completeExceptionally(new IllegalStateException("Session not connected"));
            return future;
        }

        CompletableFuture<HashMap<String, Object>> callFuture = mSession.call(
                "xbr.marketmaker.open_payment_channel",
                new TypeReference<HashMap<String, Object>>() {},
                buyerAddr,
                mEthAddr,
                amount,
                new SecureRandom(new byte[64]));
        callFuture.whenComplete((paymentChannel, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                BigInteger remaining = new BigInteger((byte[]) paymentChannel.get("remaining"));
                HashMap<String, Object> res = new HashMap<>();
                res.put("amount", paymentChannel.get("amount"));
                res.put("remaining", remaining);
                res.put("inflight", paymentChannel.get("inflight"));
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
            byte[] signature = Hash.sha256(new byte[64]);
            byte[] key = new byte[16];
            sodium().randombytes(key, 16);

            CompletableFuture<HashMap<String, Object>> callFuture = mSession.call(
                    "xbr.marketmaker.buy_key",
                    new TypeReference<HashMap<String, Object>>() {},
                    mEthAddr,
                    mEthPublicKey,
                    key,
                    mMaxPrice,
                    mMaxPrice,
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
