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

import org.json.JSONException;
import org.libsodium.jni.SodiumConstants;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import xbr.network.crypto.SealedBox;
import xbr.network.crypto.SecretBox;

import static org.libsodium.jni.NaCl.sodium;

public class SimpleBuyer {

    private final byte[] mEthPrivateKey;
    private final byte[] mEthPublicKey;
    private final byte[] mEthAddr;
    private final byte[] mMarketMakerAddr;

    private final BigInteger mMaxPrice;
    private final ECKeyPair mECKey;

    private HashMap<byte[], SecretBox> mKeys;
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

        if (!mKeys.containsKey(keyID)) {
            mSession.call(
                    "xbr.marketmaker.get_quote",
                    new TypeReference<HashMap<String, Object>>() {},
                    keyID
            ).thenAccept(quote -> {
                BigInteger price = Util.toXBR((byte[]) quote.get("price"));
                // If we have the balance to buy...
                if (mRemainingBalance.compareTo(price) > 0) {
                    int channelSeq = mSeq + 1;
                    boolean isFinal = false;
                    BigInteger remainingPost = mRemainingBalance.subtract(price);
                    try {
                        byte[] signature = Util.signEIP712Data(mECKey, verifyingChainId,
                                verifyingContractAddress, currentBlock, marketOidHex, channelOidHex,
                                channelSeq, remainingPost, isFinal);
                        buyKey(keyID, channelOidRaw, channelSeq, price, remainingPost, signature,
                                ciphertext, future);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).exceptionally(throwable -> {
                future.completeExceptionally(throwable);
                return null;
            });
        } else {
            future.completeExceptionally(new ApplicationError("xbr.error.insufficient_balance"));
        }
        return future;
    }

    private void buyKey(byte[] keyID, byte[] channelOid, int channelSeq, BigInteger price,
                        BigInteger balance, byte[] signature, byte[] ciphertext,
                        CompletableFuture<Object> future) {
        mSession.call(
                "xbr.marketmaker.buy_key", new TypeReference<HashMap<String, Object>>() {},
                mEthAddr, mPublicKey, keyID, channelOid, channelSeq,
                Numeric.toBytesPadded(price, 32), Numeric.toBytesPadded(balance, 32), signature
        ).thenAccept(receipt -> {

            // FIXME::
            int currentBlock = 1;
            int verifyingChainId = (int) mMakerConfig.get("verifying_chain_id");
            String verifyingContractAddress = (String) mMakerConfig.get("verifying_contract_adr");
            byte[] channelOidRaw = (byte[]) mChannel.get("channel_oid");
            String channelOidHex = Numeric.toHexString(channelOidRaw);
            byte[] marketOidRaw = (byte[]) mChannel.get("market_oid");
            String marketOidHex = Numeric.toHexString(marketOidRaw);

            int remoteSeq = (int) receipt.get("channel_seq");
            byte[] remaining = (byte[]) receipt.get("remaining");
            String signer = Util.recoverEIP712Signer(verifyingChainId, verifyingContractAddress,
                    currentBlock, marketOidHex, channelOidHex, (int) receipt.get("channel_seq"),
                    new BigInteger(remaining), false, (byte[]) receipt.get("signature"));

            if (!signer.equals(Numeric.toHexString(mMarketMakerAddr))) {
                System.out.println("Shit went south, I am out...");
                mSession.leave();
            } else {
                mSeq = remoteSeq;
                mRemainingBalance = Util.toXBR(receipt.get("remaining"));
                SealedBox box = new SealedBox(mPublicKey, mPrivateKey);
                byte[] key = box.decrypt((byte[]) receipt.get("sealed_key"));
                SecretBox secretBox = new SecretBox(key);
                byte[] message = secretBox.decrypt(ciphertext);
                ObjectMapper mapper = new ObjectMapper(new CBORFactory());
                try {
                    Object result = mapper.readValue(message, Object.class);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }
}
