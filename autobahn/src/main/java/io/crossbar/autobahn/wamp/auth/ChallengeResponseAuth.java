package io.crossbar.autobahn.wamp.auth;

import java.io.UnsupportedEncodingException;
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
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        String ch = challenge.extra.get("challenge").toString();
        String hash = null;
        try {
            hash = encodeToString(ch.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(new ChallengeResponse(hash, authextra));
    }

    private String encodeToString(byte[] challenge) {
        if (Platform.isAndroid()) {
            return android.util.Base64.encodeToString(sha256_HMAC.doFinal(challenge),
                    android.util.Base64.DEFAULT).trim();
        } else {
            // XXX - this adds a dependency on Java8 for non-android systems.
            // maybe find a way that works everywhere ?
            return java.util.Base64.getEncoder().encodeToString(challenge).trim();
        }
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
