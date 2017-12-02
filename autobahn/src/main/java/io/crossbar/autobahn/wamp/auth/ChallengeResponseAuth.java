package io.crossbar.autobahn.wamp.auth;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.utils.Platform;

public class ChallengeResponseAuth implements IAuthenticator {
    public static final String authmethod = "wampcra";

    public final String authid;
    public final Map<String, Object> authextra;
    public final String secret;

    private Mac sha256_HMAC;
    private Class mBase64Class;

    public ChallengeResponseAuth(String authid, String secret) {
        this(authid, secret, null);
    }

    public ChallengeResponseAuth(String authid, String secret, Map<String, Object> authextra) {
        this.authid = authid;
        this.secret = secret;
        this.authextra = authextra;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(secretKey);
            if (Platform.isAndroid()) {
                mBase64Class = Class.forName("android.util.Base64");
            } else {
                // XXX - this adds a dependency on Java8 for non-android systems.
                // maybe find a way that works everywhere ?
                mBase64Class = Class.forName("java.util.Base64");
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException
                | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        String ch = challenge.extra.get("challenge").toString();
        String hash = null;
        try {
            hash = encodeToString(ch.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(new ChallengeResponse(hash, authextra));
    }

    private String encodeToString(byte[] challenge) throws Exception {
        if (Platform.isAndroid()) {
            // Base64.encodeToString(sha256_HMAC.doFinal(challenge), Base64.DEFAULT).trim()
            // Equivalent of the above commented line of code but using reflections
            // so that this class works on both android and non-android systems.
            Method method = mBase64Class.getMethod("encodeToString", byte[].class, int.class);
            Field field = mBase64Class.getField("DEFAULT");
            String result = (String) method.invoke(null, sha256_HMAC.doFinal(challenge),
                    field.getInt(mBase64Class));
            return result.trim();
        } else {
            // Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(challenge)).trim();
            Object encoderObject = mBase64Class.getMethod("getEncoder").invoke(null);
            String result = (String) encoderObject.getClass().getMethod(
                    "encodeToString", byte[].class).invoke(
                    encoderObject, sha256_HMAC.doFinal(challenge));
            return result.trim();
        }
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
