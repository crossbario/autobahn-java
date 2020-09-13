package io.crossbar.autobahn.wamp.auth;

import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.keys.SigningKey;
import org.libsodium.jni.keys.VerifyKey;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.utils.Pair;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;

import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;

public class CryptosignAuth implements IAuthenticator {
    public static final String authmethod = "cryptosign";

    public final String authid;
    public final String authrole;
    public final Map<String, Object> authextra;

    private final byte[] privateKeyRaw;

    public static Pair<String, String> generateSigningKeyPair() {
        VerifyKey key = new VerifyKey(new Random().randomBytes(SECRETKEY_BYTES));
        SigningKey signingKey = new SigningKey(key.toBytes());

        String privateKey = AuthUtil.toHexString(key.toBytes());
        String publicKey = AuthUtil.toHexString(signingKey.getVerifyKey().toBytes());

        return new Pair<>(publicKey, privateKey);
    }

    public CryptosignAuth(String authid, String privateKey) {
        this(authid, privateKey, getPublicKey(AuthUtil.toBinary(privateKey)));
    }

    public static String getPublicKey(byte[] privateKeyRaw) {
        SigningKey signingKey = new SigningKey(privateKeyRaw);
        return AuthUtil.toHexString(signingKey.getVerifyKey().toBytes());
    }

    public CryptosignAuth(String authid, String privkey, Map<String, Object> authextra) {
        this(authid, null, privkey, authextra);
    }

    public CryptosignAuth(String authid, String privkey, String pubkey) {
        this(authid, null, privkey, new HashMap<String, Object>() {{ put("pubkey", pubkey); }});
    }

    public CryptosignAuth(String authid, File privateKeyFile) {
        this.authid = authid;
        try {
            Map<String, byte[]> keydata = AuthUtil.parseOpenSSHFile(privateKeyFile);
            Map<String, Object> authextra = new HashMap<>();
            authextra.put("pubkey", AuthUtil.toHexString(keydata.get("pubkey")));
            this.authextra = authextra;
            privateKeyRaw = keydata.get("privkey");
            this.authrole = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CryptosignAuth(String authid, String authrole, String privkey,
                          Map<String, Object> authextra) {
        this.authid = authid;
        this.authrole = authrole;
        this.authextra = authextra;
        try {
            privateKeyRaw = AuthUtil.toBinary(privkey);
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
