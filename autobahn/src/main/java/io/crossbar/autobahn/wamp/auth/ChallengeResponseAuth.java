package io.crossbar.autobahn.wamp.auth;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
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

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    public final String authid;
    public final String authrole;
    public final Map<String, Object> authextra;
    public final String secret;

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
    }

    private byte[] pbkdf2(String password, String salt, int iterations, int keySize) throws UnsupportedEncodingException {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password.getBytes("UTF-8"), salt.getBytes("UTF-8"), iterations);
        return ((KeyParameter) gen.generateDerivedParameters(keySize * 8)).getKey();
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        try {
            String ch = (String) challenge.extra.get("challenge");
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            Key secretKey;

            if (challenge.extra.containsKey("salt")) {

                // FIXME: this is broken
                byte[] key = pbkdf2(secret, (String) challenge.extra.get("salt"),
                        (int) challenge.extra.get("iterations"), (int) challenge.extra.get("keylen"));
                secretKey = new SecretKeySpec(key, "HmacSHA256");

            } else {
                secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            }

            sha256HMAC.init(secretKey);
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
