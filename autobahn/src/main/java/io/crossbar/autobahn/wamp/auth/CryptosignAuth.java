package io.crossbar.autobahn.wamp.auth;

import org.libsodium.jni.keys.SigningKey;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.utils.AuthUtil;

public class CryptosignAuth implements IAuthenticator {
    public static final String authmethod = "cryptosign";

    public final String authid;
    public final Map<String, Object> authextra;
    public final String privkey;

    private final byte[] privateKeyRaw;

    public CryptosignAuth(String authid, String privkey) {
        this(authid, privkey, null);
    }

    public CryptosignAuth(String authid, String privkey, Map<String, Object> authextra) {
        this.authid = authid;
        this.privkey = privkey;
        this.authextra = authextra;
        try {
            privateKeyRaw = AuthUtil.decodeString(privkey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        String hexChallenge = (String) challenge.extra.get("challenge");
        byte[] rawChallenge = AuthUtil.toBinary(hexChallenge);

        SigningKey key = new SigningKey(privateKeyRaw);
        byte[] signed = key.sign(rawChallenge);

        String signatureHex = AuthUtil.toHexString(signed);
        String res = signatureHex + hexChallenge;

        return CompletableFuture.completedFuture(new ChallengeResponse(res, null));
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
