package io.crossbar.autobahn.wamp.auth;

import org.libsodium.jni.keys.SigningKey;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.utils.Platform;

public class CryptosignAuth implements IAuthenticator {
    public static final String authmethod = "cryptosign";

    public final String authid;
    public final Map<String, Object> authextra;
    public final String privkey;

    private Class mBase64Class;

    public CryptosignAuth(String authid, String privkey) {
        this(authid, privkey, null);
    }

    public CryptosignAuth(String authid, String privkey, Map<String, Object> authextra) {
        this.authid = authid;
        this.privkey = privkey;
        this.authextra = authextra;
        try {
            if (Platform.isAndroid()) {
                mBase64Class = Class.forName("android.util.Base64");
            } else {
                mBase64Class = Class.forName("java.util.Base64");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        String privateKey = "GJG8MKm/s4sk6uIM/ACynPO8rLeWEw3JDyvDQVoM7xY=";
        try {
            byte[] privateKeyBytes = decodeString(privateKey);
            byte[] rawChallenge = getBinary((String) challenge.extra.get("challenge"));

            SigningKey key = new SigningKey(privateKeyBytes);
            byte[] signed = key.sign(rawChallenge);

            String signatureHex = asHex(signed);
            String dataHex = asHex(rawChallenge);
            String res = signatureHex+dataHex;

            return CompletableFuture.completedFuture(new ChallengeResponse(res, null));
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new ChallengeResponse(null, null));
        }
    }

    private byte[] getBinary(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                    Character.digit(s.charAt(i + 1), 16));
        return data;
    }

    private String asHex(byte[] buf) {
        Formatter formatter = new Formatter();
        for (byte b : buf)
            formatter.format("%02x", b);
        return formatter.toString();
    }

    private byte[] decodeString(String privateKey) throws Exception {
        if (Platform.isAndroid()) {
            Method method = mBase64Class.getMethod("decode", String.class, int.class);
            Field field = mBase64Class.getField("DEFAULT");
            return (byte[]) method.invoke(null, privateKey, field.getInt(mBase64Class));
        } else {
            Object encoderObject = mBase64Class.getMethod("getDecoder").invoke(null);
            return (byte[]) encoderObject.getClass().getMethod(
                    "decode", String.class).invoke(encoderObject, privateKey);
        }
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
