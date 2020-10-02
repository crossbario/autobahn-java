package io.crossbar.autobahn.wamp.auth;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.utils.AuthUtil;

public class ChallengeResponseAuth implements IAuthenticator {
    public static final String authmethod = "wampcra";

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

    private byte[] pbkdf2(String password, String salt, int iterations, int keySize) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8),
                iterations);
        return ((KeyParameter) gen.generateDerivedParameters(keySize * 8)).getKey();
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        try {
            String key;

            if (challenge.extra.containsKey("salt")) {
                byte[] keyRaw = pbkdf2(secret, (String) challenge.extra.get("salt"),
                        (int) challenge.extra.get("iterations"), (int) challenge.extra.get("keylen"));
                // IMPORTANT
                // Don't use the above byte[] directly, while constructing SecretKeySpec object
                // that results in wrong signature generation due to some unknown reason.
                key = AuthUtil.encodeToString(keyRaw);
            } else {
                key = secret;
            }

            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            Key secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), sha256HMAC.getAlgorithm());
            sha256HMAC.init(secretKey);

            String ch = (String) challenge.extra.get("challenge");
            String hash = AuthUtil.encodeToString(sha256HMAC.doFinal(ch.getBytes(StandardCharsets.UTF_8)));

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
