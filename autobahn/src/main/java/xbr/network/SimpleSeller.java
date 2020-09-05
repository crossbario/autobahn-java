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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONException;
import org.libsodium.jni.crypto.Random;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Registration;

public class SimpleSeller {

    private static final String TAG = SimpleSeller.class.getName();

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_STARTED = 2;
    private static final int STATE_STOPPING = 3;
    private static final int STATE_STOPPED = 4;

    private final ECKeyPair mECKey;
    private final byte[] mMarketMakerAddr;
    private final byte[] mAddr;

    private int mState;

    private HashMap<String, KeySeries> mKeys;
    private HashMap<String, KeySeries> mKeysMap;
    private Session mSession;
    private List<Registration> mSessionRegs;
    private boolean mRunning;

    private BigInteger mBalance;
    private int mSeq;
    private HashMap<String, Object> mChannel;
    private HashMap<String, Object> mPayingBalance;
    private Map<String, Object> mMakerConfig;

    private SimpleSeller(byte[] marketMakerAddr, byte[] sellerKey) {
        mState = STATE_NONE;

        mMarketMakerAddr = marketMakerAddr;
        mECKey = ECKeyPair.create(sellerKey);
        mAddr = Numeric.hexStringToByteArray(Keys.getAddress(mECKey));

        mKeys = new HashMap<>();
        mKeysMap = new HashMap<>();
        mSessionRegs = new ArrayList<>();
    }

    public SimpleSeller(String marketMakerAddr, String sellerKey) {
        this(Numeric.hexStringToByteArray(marketMakerAddr),
                Numeric.hexStringToByteArray(sellerKey));
    }

    public byte[] getPublicKey() {
        return mECKey.getPublicKey().toByteArray();
    }

    private void onRotate(KeySeries series) {
        mKeysMap.put(Numeric.toHexString(series.getID()), series);
        long validFrom = Math.round(System.nanoTime() - 10 * Math.pow(10, 9));
        byte[] signature = new Random().randomBytes(65);

        List<Object> args = new ArrayList<>();
        args.add(series.getID());
        args.add(series.getAPIID());
        args.add(series.getPrefix());
        args.add(validFrom);
        args.add(mAddr);
        args.add(signature);

        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("price", series.getPrice());
        kwargs.put("provider_id", Numeric.toHexStringWithPrefix(mECKey.getPublicKey()));

        CompletableFuture<CallResult> future = mSession.call(
                "xbr.marketmaker.place_offer", args, kwargs, new CallOptions(1000));
        future.whenComplete((callResult, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Offer placed...");
            }
        });
    }

    public void add(byte[] apiID, String prefix, BigInteger price, int interval) {
        mKeys.put(Numeric.toHexString(apiID),
                new KeySeries(apiID, price, interval, prefix, this::onRotate));
    }

    public CompletableFuture<BigInteger> start(Session session) {
        CompletableFuture<BigInteger> future = new CompletableFuture<>();

        mState = STATE_STARTING;
        mSession = session;

        String provider = Numeric.toHexStringWithPrefix(mECKey.getPublicKey());
        String procedureSell = String.format("xbr.provider.%s.sell", provider);
        String procedureCloseChannel = String.format("xbr.provider.%s.close_channel", provider);
        mSession.register(procedureSell, this::sell).thenCompose(registration -> {
            mSessionRegs.add(registration);
            return mSession.register(procedureCloseChannel, this::closeChannel);
        }).thenCompose(registration -> {
            mSessionRegs.add(registration);

            for (KeySeries series: mKeys.values()) {
                series.start();
            }

            return session.call("xbr.marketmaker.get_config", Map.class);

        }).thenCompose(makerConfig -> {

            mMakerConfig = makerConfig;

            return mSession.call(
                    "xbr.marketmaker.get_active_paying_channel",
                    new TypeReference<HashMap<String, Object>>() {},
                    mAddr);
        }).thenCompose(channel -> {
            mChannel = channel;
            return mSession.call(
                    "xbr.marketmaker.get_paying_channel_balance",
                    new TypeReference<HashMap<String, Object>>() {},
                    channel.get("channel_oid"));
        }).thenAccept(payingBalance -> {
            mSeq = (int) payingBalance.get("seq");
            mBalance = new BigInteger((byte[]) payingBalance.get("remaining"));
            mState = STATE_STARTED;
            future.complete(mBalance);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });

        return future;
    }

    public InvocationResult sell(List<Object> args, Map<String, Object> kwargs,
                                 InvocationDetails details) {

        String marketMakerAddr = Numeric.toHexString((byte[]) args.get(0));
        byte[] buyerPubKey = (byte[]) args.get(1);
        byte[] keyIDRaw = (byte[]) args.get(2);
        String keyID = Numeric.toHexString(keyIDRaw);
        byte[] channelOidRaw = (byte[]) args.get(3);
        String channelOid = Numeric.toHexString(channelOidRaw);
        int channelSeq = (int) args.get(4);
        byte[] amountRaw = (byte[]) args.get(5);
        BigInteger amount = new BigInteger(amountRaw);
        byte[] balanceRaw = (byte[]) args.get(6);
        BigInteger balance = new BigInteger(balanceRaw);
        byte[] signature = (byte[]) args.get(7);

        if (!marketMakerAddr.equals(Numeric.toHexString(mMarketMakerAddr))) {
            throw new ApplicationError("xbr.error.unexpected_marketmaker_adr");
        }

        if (!mKeysMap.containsKey(keyID)) {
            throw new ApplicationError("crossbar.error.no_such_object");
        }

        // FIXME::
        int currentBlock = 1;
        int verifyingChainId = (int) mMakerConfig.get("verifying_chain_id");
        String verifyingContractAddress = (String) mMakerConfig.get("verifying_contract_adr");
        byte[] marketOidRaw = (byte[]) mChannel.get("market_oid");
        String marketOid = Numeric.toHexString(marketOidRaw);

        String signerAddr = Util.recoverEIP712Signer(verifyingChainId, verifyingContractAddress,
                currentBlock, marketOid, channelOid, channelSeq, balance, false, signature);

        if (!signerAddr.equals(marketMakerAddr)) {
            throw new ApplicationError("xbr.error.invalid_signature");
        }

        mSeq += 1;
        mBalance = mBalance.subtract(amount);

        KeySeries series = mKeysMap.get(keyID);
        byte[] sealedKey = series.encryptKey(keyIDRaw, buyerPubKey);

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("key_id", keyIDRaw);
        receipt.put("delegate", mAddr);
        receipt.put("buyer_pubkey", buyerPubKey);
        receipt.put("sealed_key", sealedKey);
        receipt.put("channel_seq", mSeq);
        receipt.put("amount", amountRaw);
        receipt.put("balance", mBalance.toByteArray());

        try {
            byte[] sellerSignature = Util.signEIP712Data(mECKey, verifyingChainId,
                    verifyingContractAddress, currentBlock, marketOid, channelOid, mSeq,
                    mBalance, false);
            receipt.put("signature", sellerSignature);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return new InvocationResult((Object) receipt);
    }

    public String closeChannel(List<Object> args, Map<String, Object> kwargs,
                               InvocationDetails details) {
        System.out.println(args);
        System.out.println(kwargs);
        return null;
    }

    public Map<String, Object> wrap(byte[] apiID, String uri, Map<String, Object> payload)
            throws JsonProcessingException {
        KeySeries series = mKeys.get(Numeric.toHexString(apiID));
        return series.encrypt(payload);
    }
}
