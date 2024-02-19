package io.crossbar.autobahn.wamp.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.utils.Pair;
import io.crossbar.autobahn.wamp.types.Challenge;
import io.crossbar.autobahn.wamp.types.ChallengeResponse;

public class CryptosignAuthTests {

    private static final String TestAuthID = "testAuthId";
    private static final String TestPrivateKey = "61b297d1573d0a2a6ac58d7fd39369adbd365c5b3276bd69edf661c92b7ad9ff";
    private static final String TestPublicKey = "ea971c008ee99021eaf48342791442dd742259a4bf14004fa3500d1fa6995211";

    @Test
    public void testGenerateSigningKeyPair() {
        Pair<String, String> keyPair = CryptosignAuth.generateSigningKeyPair();
        assertNotNull(keyPair.first);
        assertNotNull(keyPair.second);
        assertEquals(64, keyPair.first.length());
        assertEquals(64, keyPair.second.length());
    }

    @Test
    public void testGetPublicKey() {
        byte[] privateKeyRaw = AuthUtil.toBinary(TestPrivateKey);
        String publicKeyHex = CryptosignAuth.getPublicKey(privateKeyRaw);
        assertNotNull(publicKeyHex);
        assertEquals(TestPublicKey, publicKeyHex);
    }

    @Test
    public void testConstructorWithPrivateKey() {
        CryptosignAuth auth = new CryptosignAuth(TestAuthID, TestPrivateKey);
        assertNotNull(auth);
        assertEquals(TestAuthID, auth.authid);
    }

    @Test
    public void testConstructorWithPrivateKeyAndPubKey() {
        CryptosignAuth auth = new CryptosignAuth(TestAuthID, TestPrivateKey, TestPublicKey);
        assertNotNull(auth);
        assertEquals(TestAuthID, auth.authid);
    }

    @Test
    public void testConstructorWithPrivateKeyFile() throws IOException {
        File privateKeyFile = createPrivateKeyFile();
        CryptosignAuth auth = new CryptosignAuth(TestAuthID, privateKeyFile);
        assertNotNull(auth);
        assertEquals(TestAuthID, auth.authid);
    }

    private File createPrivateKeyFile() throws IOException {
        String privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAgwAAAAtzc2gtZW\n" +
                "QyNTUxOQAAACCxFVfOJU6fDhJmG1Hh8qW2nEdqeDbXLkCOoeepu5VxcAAACBAMP3yQT9Mk\n" +
                "/YAAAAAtzc2gtZWQyNTUxOQAAACCxFVfOJU6fDhJmG1Hh8qW2nEdqeDbXLkCOoeepu5Vxc\n" +
                "AAAEA6Ay3awSTmEnMS7QZi4ofEiChSju2bN8wFFS4tmFrG3sVV84lTp8OEmYbUeHypbacR\n" +
                "2p4NtcuQI6h56m7lXFwAAAECf4qghufu/GJScYXWHjVPfX+znr0NQ1p2YVNTZi7mC0rLQ6\n" +
                "qSsWfNYu4mSvA1Q6Jv6Xu8HAB/TIllo5bqV9acxSbA5GEE0S8T6rMq0BtZk+jHxUlFMjrr\n" +
                "QwEW18H/mFjtsitG8K5XWfBgMb3dF8XsoGYJyKlY4e4p0D7BBjqSKnB7wlUGwkCdTeeUDw==\n" +
                "-----END OPENSSH PRIVATE KEY-----\n";
        byte[] privateKeyBytes = privateKey.getBytes(StandardCharsets.UTF_8);
        Path tempFilePath = Files.createTempFile("private_key", ".txt");
        Files.write(tempFilePath, privateKeyBytes);
        return tempFilePath.toFile();
    }

    @Test
    public void testConstructorWithAuthIdPrivateKeyAuthExtra() {
        Map<String, Object> authextra = new HashMap<>();
        authextra.put("pubkey", TestPublicKey);
        CryptosignAuth auth = new CryptosignAuth(TestAuthID, TestPrivateKey, authextra);
        assertNotNull(auth);
        assertEquals(TestAuthID, auth.authid);
        assertEquals(authextra, auth.authextra);
    }

    @Test
    public void testOnChallenge() {
        String challengeString = "f9d17535fb925e9f674d648cbfc41399";
        String signedString = "539707667d93bb9eb01e72be9ca5e00006bb6b1b786d697b3f189ebf5a0f60c70" +
                "b8054f3735e19b77df31dc990864fb21259cfe3021f9a7e8ec0427c2077840a";
        Challenge challenge = new Challenge("cryptosign", new HashMap<String, Object>() {{
            put("challenge", challengeString);
        }});

        CryptosignAuth auth = new CryptosignAuth(TestAuthID, TestPrivateKey);
        ChallengeResponse response = auth.onChallenge(null, challenge).join();
        assertNotNull(response.signature);
        assertEquals(signedString + challengeString, response.signature);
    }

    @Test
    public void testGetAuthMethod() {
        CryptosignAuth auth = new CryptosignAuth(TestAuthID, TestPrivateKey);
        assertEquals("cryptosign", auth.getAuthMethod());
    }
}