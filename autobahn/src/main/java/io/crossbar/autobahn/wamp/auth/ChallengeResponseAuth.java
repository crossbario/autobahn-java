package io.crossbar.autobahn.wamp.auth;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.utils.AuthUtil;

public class ChallengeResponseAuth implements IAuthenticator {
    public static final String authmethod = "wampcra";

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    public final String authid;
    public final String authrole;
    public final Map<String, Object> authextra;
    public final String secret;

    private Mac sha256HMAC;

    public ChallengeResponseAuth(String authid, String secret) {
        this(authid, secret, null, null);
    }

    public ChallengeResponseAuth(String authid, String secret, Map<String, Object> authextra) {
        this(authid, secret, null, authextra);
    }

    public ChallengeResponseAuth(String authid, String secret, String authrole,
                                 Map<String, Object> authextra) {
        this.authid = authid;
        this.authrole = authrole;
        this.secret = secret;
        this.authextra = authextra;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC = Mac.getInstance("HmacSHA256");
            sha256HMAC.init(secretKey);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        String ch = (String) challenge.extra.get("challenge");
        try {
            if (challenge.extra.containsKey("salt")) {
                byte[] key = pbkdf2(ch.toCharArray(), (byte[]) challenge.extra.get("salt"),
                        (int) challenge.extra.get("iterations"), (int) challenge.extra.get("keylen"));
                ch = AuthUtil.toHexString(key);
            }
            String hash = AuthUtil.encodeToString(sha256HMAC.doFinal(ch.getBytes("UTF-8")));
            return CompletableFuture.completedFuture(new ChallengeResponse(hash, authextra));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
