package io.crossbar.autobahn.wamp.auth;

import com.neilalexander.jnacl.NaCl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;

public class CryptosignAuth implements IAuthenticator {
    public static final String authmethod = "cryptosign";

    public final String authid;
    public final Map<String, Object> authextra;
    public final String privkey;

    public CryptosignAuth(String authid, String privkey) {
        this(authid, privkey, null);
    }

    public CryptosignAuth(String authid, String privkey, Map<String, Object> authextra) {
        this.authid = authid;
        this.privkey = privkey;
        this.authextra = authextra;
    }

    @Override
    public CompletableFuture<ChallengeResponse> onChallenge(Session session, Challenge challenge) {
        System.out.println(hexToString((String) challenge.extra.get("challenge")).length());
        NaCl naCl = new NaCl();
        naCl.
        return null;
    }

    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        char[] hexData = hex.toCharArray();
        for (int count = 0; count < hexData.length - 1; count += 2) {
            int firstDigit = Character.digit(hexData[count], 16);
            int lastDigit = Character.digit(hexData[count + 1], 16);
            int decimal = firstDigit * 16 + lastDigit;
            sb.append((char)decimal);
        }
        return sb.toString();
    }

    @Override
    public String getAuthMethod() {
        return authmethod;
    }
}
