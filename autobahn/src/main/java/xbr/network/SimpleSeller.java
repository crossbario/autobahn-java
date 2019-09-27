package xbr.network;

import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;

import static org.libsodium.jni.encoders.Encoder.HEX;

public class SimpleSeller {
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_STARTED = 2;
    private static final int STATE_STOPPING = 3;
    private static final int STATE_STOPPED = 4;

    private final byte[] mEthPrivateKey;
    private final byte[] mEthPublicKey;
    private final ECKey mECKey;
    private final byte[] mMarketMakerAddr;
    private final byte[] mPrivateKeyRaw;

    private int mState;

    private HashMap<byte[], KeySeries> mKeys;
    private HashMap<byte[], KeySeries> mKeysMap;
    private Session mSession;
    private boolean mRunning;

    private long mRemainingBalance;
    private HashMap<String, Object> mChannel;

    public SimpleSeller(byte[] marketMakerAddr, byte[] sellerKey) {
        mState = STATE_NONE;

        mMarketMakerAddr = marketMakerAddr;

        mPrivateKeyRaw = sellerKey;
        mECKey = ECKey.fromPrivate(sellerKey);
        mEthPrivateKey = mECKey.getPrivKeyBytes();
        mEthPublicKey = ECKey.publicKeyFromPrivate(mECKey.getPrivKey(), false);

        mKeys = new HashMap<>();
        mKeysMap = new HashMap<>();

    }

    byte[] getPublicKey() {
        return mEthPublicKey;
    }

    private void onRotate(KeySeries series, String prefix) {
        mKeysMap.put(series.getID(), series);
        double validFrom = System.nanoTime() - 10 * Math.pow(10, 9);
        byte[] signature = new byte[65];
        new Random().nextBytes(signature);

        List<Object> args = new ArrayList<>();
        args.add(series.getID());
        args.add(series.getAPIID());
        args.add(series.getPrefix());
        args.add(validFrom);
        // FIXME
//        args.add(delegate);
        args.add(signature);

        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("privkey", null);
        kwargs.put("price", series.getPrice());
        kwargs.put("categories", null);
        kwargs.put("expires", null);
        kwargs.put("copies", null);
        // FIXME
        kwargs.put("provider_id", null);

        CompletableFuture<CallResult> future = mSession.call(
                "xbr.marketmaker.place_offer", args, kwargs);
        future.whenComplete((callResult, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }

    void add(byte[] apiID, String prefix, int price, int interval) {
        KeySeries series = new KeySeries(apiID, price, interval, prefix, this::onRotate);
        mKeys.put(apiID, series);
    }

    void start(Session session) {
        mState = STATE_STARTING;
        mSession = session;

        for (KeySeries series: mKeys.values()) {
            series.start();
        }
    }

    public String sell(byte[] marketMakerAddr, byte[] buyerPubKey, byte[] keyID,
                       byte[] channelAddr, int channelSeq, byte[] amount, byte[] balance,
                       byte[] signature) {
        if (!mKeysMap.containsKey(keyID)) {
            throw new ApplicationError("crossbar.error.no_such_object");
        }
        SealedBox box = new SealedBox(buyerPubKey);
        return HEX.encode(box.encrypt(mKeysMap.get(keyID)));
    }
}
