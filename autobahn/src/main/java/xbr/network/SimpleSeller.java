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
import io.crossbar.autobahn.wamp.types.Registration;

public class SimpleSeller {
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

    private long mRemainingBalance;
    private HashMap<String, Object> mChannel;

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

    byte[] getPublicKey() {
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
        kwargs.put("privkey", null);
        kwargs.put("price", series.getPrice());
        kwargs.put("categories", null);
        kwargs.put("expires", null);
        kwargs.put("copies", null);
        kwargs.put("provider_id", Numeric.toHexString(mAddr));

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

    public void start(Session session) {
        mState = STATE_STARTING;
        mSession = session;

        String provider = Numeric.prependHexPrefix(Keys.getAddress(mECKey));
        String procedureSell = String.format("xbr.provider.%s.sell", provider);
        mSession.register(procedureSell, this::sell).thenAccept(registration -> {
            mSessionRegs.add(registration);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

        String procedureCloseChannel = String.format("xbr.provider.%s.close_channel", provider);
        mSession.register(procedureCloseChannel, this::closeChannel).thenAccept(registration -> {
            mSessionRegs.add(registration);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

        for (KeySeries series: mKeys.values()) {
            series.start();
        }

        mSession.call(
                "xbr.marketmaker.get_active_paying_channel",
                new TypeReference<HashMap<String, Object>>() {},
                mAddr
        ).thenCompose(channel -> mSession.call(
                "xbr.marketmaker.get_paying_channel_balance",
                new TypeReference<HashMap<String, Object>>() {},
                channel.get("channel"))
        ).thenAccept(payingBalance -> {
            byte[] remaining = (byte[]) payingBalance.get("remaining");
            BigInteger bi = new BigInteger("10").pow(18);
            System.out.println(Numeric.toBigInt(remaining).divide(bi));
            mState = STATE_STARTED;
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    public String sell(List<Object> args, Map<String, Object> kwargs, InvocationDetails details) {
        String marketMakerAddr = Numeric.toHexString((byte[]) args.get(0));
        String keyID = Numeric.toHexString((byte[]) args.get(2));
        byte[] channelAddrRaw = (byte[]) args.get(3);
        String channelAddr = Numeric.toHexString(channelAddrRaw);
        int channelSeq = (int) args.get(4);
        BigInteger amount = new BigInteger((byte[]) args.get(5));
        BigInteger balance = new BigInteger((byte[]) args.get(6));
        byte[] signature = (byte[]) args.get(7);

        if (!marketMakerAddr.equals(Numeric.toHexString(mMarketMakerAddr))) {
            throw new ApplicationError("xbr.error.unexpected_marketmaker_adr");
        }

        if (!mKeysMap.containsKey(keyID)) {
            throw new ApplicationError("crossbar.error.no_such_object");
        }

        try {
            String signerAddr = Util.recoverEIP712Signer(channelAddrRaw, channelSeq,
                    balance, false, signature);
            System.out.println(signerAddr);
            System.out.println(Numeric.toHexString(mMarketMakerAddr));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        KeySeries series = mKeysMap.get(keyID);


//        if (!mKeysMap.containsKey(keyID)) {
//            throw new ApplicationError("crossbar.error.no_such_object");
//        }
//        SealedBox box = new SealedBox(buyerPubKey);
////        return HEX.encode(box.encrypt(mKeysMap.get(keyID)));
//        return null;
        return null;
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
