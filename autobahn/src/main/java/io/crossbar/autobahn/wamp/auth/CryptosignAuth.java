package io.crossbar.autobahn.wamp.auth;

import org.libsodium.jni.keys.SigningKey;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;
import io.crossbar.autobahn.wamp.utils.AuthUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class CryptosignAuth implements IAuthenticator {
    public static final String authmethod = "cryptosign";

    public final String authid;
    public final Map<String, Object> authextra;

    private final byte[] privateKeyRaw;

    public CryptosignAuth(String authid, String privkey, Map<String, Object> authextra) {
        this.authid = authid;
        if (authextra == null || getOrDefault(authextra, "pubkey", null) == null) {
            throw new RuntimeException("authextra must contain pubkey");
        }
        this.authextra = authextra;
        try {
            privateKeyRaw = AuthUtil.toBinary(privkey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CryptosignAuth(String authid, String privkey, String pubkey) {
        Map<String, Object> authextra = new HashMap<>();
        authextra.put("pubkey", pubkey);
        this.authid = authid;
        this.authextra = authextra;
        try {
            privateKeyRaw = AuthUtil.toBinary(privkey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CryptosignAuth(String authid, File privateKeyFile) {
        this.authid = authid;
        try {
            Map<String, byte[]> keydata = AuthUtil.parseOpenSSHFile(privateKeyFile);
            Map<String, Object> authextra = new HashMap<>();
            authextra.put("pubkey", AuthUtil.toHexString(keydata.get("pubkey")));
            this.authextra = authextra;
            privateKeyRaw = keydata.get("privkey");
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
