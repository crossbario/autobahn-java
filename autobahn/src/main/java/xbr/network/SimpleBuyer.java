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

package xbr.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.libsodium.jni.SodiumConstants;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import xbr.network.crypto.SealedBox;
import xbr.network.crypto.SecretBox;
import xbr.network.pojo.Quote;
import xbr.network.pojo.Receipt;

import static org.libsodium.jni.NaCl.sodium;

public class SimpleBuyer {

    private static final IABLogger LOGGER = ABLogger.getLogger(SimpleBuyer.class.getName());

    private final byte[] mEthPrivateKey;
    private final byte[] mEthPublicKey;
    private final byte[] mEthAddr;
    private final byte[] mMarketMakerAddr;

    private final BigInteger mMaxPrice;
    private final ECKeyPair mECKey;

    private HashMap<String, SecretBox> mKeys;
    private Session mSession;
    private boolean mRunning;
    private int mSeq;

    private BigInteger mRemainingBalance;
    private HashMap<String, Object> mChannel;
    private Map<String, Object> mMakerConfig;

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

        mSession.call(
                "xbr.marketmaker.get_config",
                Map.class
        ).thenCompose(makerConfig -> {
            mMakerConfig = makerConfig;
            return mSession.call(
                    "xbr.marketmaker.get_active_payment_channel",
                    new TypeReference<HashMap<String, Object>>() {},
                    mEthAddr);
        }).thenCompose(channel -> {
            mChannel = channel;
            return balance();
        }).thenAccept(balance -> {
            future.complete((BigInteger) balance.get("remaining"));
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
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

        mSession.call(
                "xbr.marketmaker.get_payment_channel_balance",
                new TypeReference<HashMap<String, Object>>() {},
                mChannel.get("channel_oid")
        ).thenAccept(paymentBalance -> {
            mRemainingBalance = new BigInteger((byte[]) paymentBalance.get("remaining"));
            HashMap<String, Object> res = new HashMap<>();
            res.put("amount", paymentBalance.get("amount"));
            res.put("remaining", mRemainingBalance);
            res.put("inflight", paymentBalance.get("inflight"));
            mSeq = (int) paymentBalance.get("seq");
            future.complete(res);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
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

        mSession.call(
                "xbr.marketmaker.open_payment_channel",
                new TypeReference<HashMap<String, Object>>() {},
                buyerAddr,
                mEthAddr,
                amount,
                new SecureRandom(new byte[64])
        ).thenAccept(paymentChannel -> {

            BigInteger remaining = new BigInteger((byte[]) paymentChannel.get("remaining"));
            HashMap<String, Object> res = new HashMap<>();
            res.put("amount", paymentChannel.get("amount"));
            res.put("remaining", remaining);
            res.put("inflight", paymentChannel.get("inflight"));
            future.complete(res);

        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });


        return future;
    }

    public void closeChannel() {

    }

    private CompletableFuture<Object> decrypt(SecretBox secretBox, byte[] ciphertext) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        try {
            byte[] message = secretBox.decrypt(ciphertext);
            ObjectMapper mapper = new ObjectMapper(new CBORFactory());
            Object result = mapper.readValue(message, Object.class);
            future.complete(result);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<Object> unwrap(byte[] keyID, String encSerializer, byte[] ciphertext) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        // FIXME::
        int currentBlock = 1;
        int verifyingChainId = (int) mMakerConfig.get("verifying_chain_id");
        String verifyingContractAddress = (String) mMakerConfig.get("verifying_contract_adr");
        byte[] channelOidRaw = (byte[]) mChannel.get("channel_oid");
        String channelOidHex = Numeric.toHexString(channelOidRaw);
        byte[] marketOidRaw = (byte[]) mChannel.get("market_oid");
        String marketOidHex = Numeric.toHexString(marketOidRaw);

        String keyHex = Numeric.toHexString(keyID);

        if (mKeys.containsKey(keyHex)) {
            LOGGER.i("Key already in store (or currently being bought)");
            SecretBox box = mKeys.get(keyHex);
            if (box != null) {
                decrypt(box, ciphertext).thenAccept(o -> {
                    future.complete(o);
                }).exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
            }
        } else {
            mKeys.put(keyHex, null);
            int channelSeq = mSeq + 1;
            boolean isFinal = false;

            AtomicReference<BigInteger> price = new AtomicReference<>();
            AtomicReference<BigInteger> remainingPostBuy = new AtomicReference<>();
            AtomicReference<Receipt> receipt = new AtomicReference<>();

            mSession.call(
                    "xbr.marketmaker.get_quote",
                    new TypeReference<Quote>() {},
                    keyID
            ).thenCompose(quote -> {
                CompletableFuture<byte[]> dFuture = new CompletableFuture<>();

                price.set(quote.getPriceBigInt());
                // If we have the balance to buy...
                if (mRemainingBalance.compareTo(price.get()) > 0) {
                    remainingPostBuy.set(mRemainingBalance.subtract(price.get()));

                    return Util.signEIP712Data(mECKey, verifyingChainId,
                            verifyingContractAddress, currentBlock, marketOidHex, channelOidHex,
                            channelSeq, remainingPostBuy.get(), isFinal);
                } else {
                    dFuture.completeExceptionally(new ApplicationError("xbr.error.insufficient_balance"));
                    return dFuture;
                }
            }).thenCompose(signature -> {
                return mSession.call(
                        "xbr.marketmaker.buy_key", new TypeReference<Receipt>() {},
                        mEthAddr, mPublicKey, keyID, channelOidRaw, channelSeq,
                        Numeric.toBytesPadded(price.get(), 32),
                        Numeric.toBytesPadded(remainingPostBuy.get(), 32), signature
                );
            }).thenCompose(receiptObj -> {
                receipt.set(receiptObj);

                return Util.recoverEIP712Signer(verifyingChainId, verifyingContractAddress,
                        currentBlock, marketOidHex, channelOidHex, receiptObj.channel_seq,
                        new BigInteger(receiptObj.remaining), false, receiptObj.signature);

            }).thenCompose(signer -> {
                if (signer == null || !signer.equals(Numeric.toHexString(mMarketMakerAddr))) {
                    mSession.leave();
                    throw new ApplicationError("xbr.error.wrong_market_maker");
                } else {
                    Receipt rec = receipt.get();
                    mSeq = rec.channel_seq;
                    mRemainingBalance = Util.toXBR(rec.remaining);
                    SealedBox box = new SealedBox(mPublicKey, mPrivateKey);
                    byte[] key = box.decrypt((rec.sealed_key));
                    SecretBox secretBox = new SecretBox(key);
                    mKeys.put(keyHex, secretBox);
                    return decrypt(secretBox, ciphertext);
                }
            }).thenAccept(o -> {
                future.complete(o);
            }).exceptionally(throwable -> {
                future.completeExceptionally(throwable);
                return null;
            });
        }
        return future;
    }
}
