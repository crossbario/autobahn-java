package xbr.network;

import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.util.HashMap;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.exceptions.ApplicationError;

import static org.libsodium.jni.encoders.Encoder.HEX;

public class SimpleSeller {
    private final String mEthPrivateKey;
    private final String mEthPublicKey;
    private final ECKey mECKey;

    private HashMap<String, String> mKeys;
    private HashMap<byte[], byte[]> mKeysMap;
    private Session mSession;
    private boolean mRunning;

    private long mRemainingBalance;
    private HashMap<String, Object> mChannel;

    public SimpleSeller(String sellerKey) {
        byte[] privatekey = Hex.decode(sellerKey);
        mECKey = ECKey.fromPrivate(privatekey);
        mEthPrivateKey = Hex.toHexString(mECKey.getPrivKeyBytes());
        mEthPublicKey = Hex.toHexString(ECKey.publicKeyFromPrivate(mECKey.getPrivKey(), false));

        mKeys = new HashMap<>();
        mKeysMap = new HashMap<>();
    }

    public String sell(byte[] keyID, byte[] buyerPubKey) {
        if (!mKeysMap.containsKey(keyID)) {
            throw new ApplicationError("crossbar.error.no_such_object", null, null);
        }
        SealedBox box = new SealedBox(buyerPubKey);
        return HEX.encode(box.encrypt(mKeysMap.get(keyID)));
    }
}
