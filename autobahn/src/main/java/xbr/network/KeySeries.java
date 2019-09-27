package xbr.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KeySeries {

    private final byte[] mAPIID;
    private final int mPrice;
    private final int mInterval;
    private final SecureRandom mRandom;

    private byte[] mID;
    private byte[] mKey;
    private SecretBox mBox;
    private Map<byte[], Map<String, Object>> mArchive;
    private ObjectMapper mCBOR;
    private BiConsumer<KeySeries, String> mOnRotateCallback;
    private Timer mTimer;
    private String mPrefix;

    private boolean mRunning = false;

    KeySeries(byte[] apiID, int price, int interval, String prefix,
              BiConsumer<KeySeries, String> onRotate) {
        mAPIID = apiID;
        mPrice = price;
        mInterval = interval;
        mCBOR = new ObjectMapper(new CBORFactory());
        mRandom = new SecureRandom();
        mOnRotateCallback = onRotate;
        mArchive = new HashMap<>();
        mTimer = new Timer();
        mPrefix = prefix;
    }

    void start() {
        mRunning = true;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onRotate();
            }
        }, 0, mInterval);
    }

    void stop() {
        mTimer.cancel();
        mRunning = false;
    }

    byte[] getID() {
        return mID;
    }

    byte[] getAPIID() {
        return mAPIID;
    }

    String getPrefix() {
        return mPrefix;
    }

    int getPrice() {
        return mPrice;
    }

    Map<String, Object> encrypt(Object payload) throws JsonProcessingException {
        byte[] nonce = new Random().randomBytes(
                SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);

        Map<String, Object> data = new HashMap<>();
        data.put("id", mID);
        data.put("serializer", "cbor");
        data.put("ciphertext", mBox.encrypt(nonce, mCBOR.writeValueAsBytes(payload)));

        return data;
    }

    byte[] encryptKey(byte[] keyID, byte[] buyerPubKey) {
        Map<String, Object> key = mArchive.get(keyID);
        SealedBox sendKeyBox = new SealedBox(buyerPubKey);
        return sendKeyBox.encrypt((byte[]) key.get("key"));
    }

    private void onRotate() {
        byte[] randomData = new byte[16];
        mRandom.nextBytes(randomData);
        mID = randomData;

        mKey = new Random().randomBytes(SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES);
        mBox = new SecretBox(mKey);
        Map<String, Object> data = new HashMap<>();
        data.put("key", mKey);
        data.put("box", mBox);
        mArchive.put(mID, data);

        mOnRotateCallback.accept(this, mPrefix);
    }
}
