package xbr.network;


import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle.jce.ECKeyUtil;
import org.bouncycastle.jce.interfaces.ECKey;
import org.json.JSONException;
import org.libsodium.jni.SodiumConstants;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.messages.Hello;
import io.reactivex.internal.util.HashMapSupplier;

import static org.libsodium.jni.NaCl.sodium;

public class SimpleBuyer {

    private final byte[] mEthPrivateKey;
    private final byte[] mEthPublicKey;
    private final byte[] mEthAddr;
    private final byte[] mMarketMakerAddr;

    private final BigInteger mMaxPrice;
    private final ECKeyPair mECKey;

    private HashMap<byte[], byte[]> mKeys;
    private Session mSession;
    private boolean mRunning;
    private int mSeq;

    private BigInteger mRemainingBalance;
    private HashMap<String, Object> mChannel;

    private final byte[] mPrivateKey;
    private final byte[] mPublicKey;

    public SimpleBuyer(String marketMakerAddr, String buyerKey, BigInteger maxPrice) {
        mECKey = ECKeyPair.create(Numeric.hexStringToByteArray(buyerKey));
        mMarketMakerAddr = Numeric.hexStringToByteArray(marketMakerAddr);
        mEthPrivateKey = mECKey.getPrivateKey().toByteArray();
        mEthPublicKey = mECKey.getPublicKey().toByteArray();
        mEthAddr = Numeric.hexStringToByteArray(Credentials.create(mECKey).getAddress());

        mPrivateKey = new byte[SodiumConstants.SECRETKEY_BYTES];
        mPublicKey = new byte[SodiumConstants.PUBLICKEY_BYTES];
        sodium().crypto_box_keypair(mPublicKey, mPrivateKey);

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
                System.out.println(paymentBalance);
                mRemainingBalance = new BigInteger((byte[]) paymentBalance.get("remaining"));
                HashMap<String, Object> res = new HashMap<>();
                res.put("amount", paymentBalance.get("amount"));
                res.put("remaining", mRemainingBalance);
                res.put("inflight", paymentBalance.get("inflight"));
                mSeq = (int) paymentBalance.get("seq");
                System.out.println(mRemainingBalance);
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

    public CompletableFuture<String> unwrap(byte[] keyID, String encSerializer, byte[] ciphertext) {
        CompletableFuture<String> future = new CompletableFuture<>();

        byte[] channelAddr = (byte[]) mChannel.get("channel");

        if (!mKeys.containsKey(keyID)) {
            CompletableFuture<HashMap<String, Object>> quoteF = mSession.call(
                    "xbr.marketmaker.get_quote",
                    new TypeReference<HashMap<String, Object>>() {}, keyID);
            quoteF.whenComplete((quote, throwable) -> {
                BigInteger price = Util.toXBR(quote.get("price"));
                // If we have the balance to buy...
                if (mRemainingBalance.compareTo(price) > 0) {
                    int channelSeq = mSeq + 1;
                    boolean isFinal = false;
                    try {
                        byte[] signature = Util.signEIP712Data(mECKey, channelAddr, channelSeq,
                                mRemainingBalance.subtract(price), isFinal);
                        BigInteger remainingPost = mRemainingBalance.subtract(price);
                        buyKey(keyID, channelAddr, channelSeq, price, remainingPost, signature);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {

        }
        return future;
    }

    private void buyKey(byte[] keyID, byte[] channelAddr, int channelSeq, BigInteger price,
                        BigInteger balance, byte[] signature) {
        CompletableFuture<HashMap<String, Object>> future = mSession.call(
                "xbr.marketmaker.buy_key", new TypeReference<HashMap<String, Object>>() {},
                mEthAddr, mPublicKey, keyID, channelAddr, channelSeq,
                Numeric.toBytesPadded(price, 32), Numeric.toBytesPadded(balance, 32), signature);
        future.whenComplete((receipt, throwable) -> {
            int remoteSeq = (int) receipt.get("channel_seq");
            String signer = Util.recoverEIP712Signer(channelAddr, (int) receipt.get("channel_seq"),
                    new BigInteger((byte[]) receipt.get("remaining")), false, signature);
            mSeq = remoteSeq;
            System.out.println(signer);
            System.out.println(Numeric.toHexString(mMarketMakerAddr));
        });
    }
}
