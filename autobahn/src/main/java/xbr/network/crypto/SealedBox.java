package xbr.network.crypto;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.engines.XSalsa20Engine;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.math.ec.rfc7748.X25519;
import org.bouncycastle.util.Arrays;
import org.libsodium.jni.encoders.Encoder;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.NONCE_BYTES;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.crypto.Util.isValid;

import io.crossbar.autobahn.utils.Pair;
import xbr.network.Util;

public class SealedBox {

    private static final int MAC_BYTES = 16;
    private static final int SEAL_BYTES = PUBLICKEY_BYTES + MAC_BYTES;

    private byte[] publicKey;
    private byte[] privateKey;

    public SealedBox(byte[] publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key must not be null");
        }
        this.publicKey = publicKey;
        this.privateKey = null;
    }

    public SealedBox(String publicKey, Encoder encoder) {
        this(encoder.decode(publicKey));
    }

    public SealedBox(byte[] publicKey, byte[] privateKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key must not be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key must not be null");
        }
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public SealedBox(String publicKey, String privateKey, Encoder encoder) {
        this(encoder.decode(publicKey), encoder.decode(privateKey));
    }

    public byte[] encrypt(byte[] message) {
        byte[] ct = new byte[message.length + SEAL_BYTES];
        isValid(sodium().crypto_box_seal(
                        ct, message, message.length, publicKey),
                "Encryption failed");
        return ct;
    }

    public byte[] encrypt(byte[] message, byte[] recipientPublicKey) {
        Pair<byte[], byte[]> keyPair = Util.generateX25519KeyPair();
        byte[] nonce = createNonce(keyPair.first, recipientPublicKey);
        byte[] sharedSecret = computeSharedSecret(recipientPublicKey, keyPair.second);

        XSalsa20Engine cipher = new XSalsa20Engine();
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(sharedSecret), nonce);
        cipher.init(true, params);

        byte[] sk = new byte[SECRETKEY_BYTES];
        cipher.processBytes(sk, 0, sk.length, sk, 0);

        // encrypt the message
        byte[] ciphertext = new byte[message.length];
        cipher.processBytes(message, 0, message.length, ciphertext, 0);

        // create the MAC
        Poly1305 mac = new Poly1305();
        byte[] macBuf = new byte[mac.getMacSize()];
        mac.init(new KeyParameter(sk));
        mac.update(ciphertext, 0, ciphertext.length);
        mac.doFinal(macBuf, 0);

        return Arrays.concatenate(keyPair.first, macBuf, ciphertext);
    }

    private byte[] createNonce(byte[] ephemeralPublicKey, byte[] recipientPublicKey) {
        Blake2bDigest blake2b = new Blake2bDigest(NONCE_BYTES * 8);
        byte[] nonce = new byte[blake2b.getDigestSize()];

        blake2b.update(ephemeralPublicKey, 0, ephemeralPublicKey.length);
        blake2b.update(recipientPublicKey, 0, recipientPublicKey.length);

        blake2b.doFinal(nonce, 0);

        return nonce;
    }

    public byte[] computeSharedSecret(byte[] publicKey, byte[] privateKey) {
        byte[] sharedSecret = new byte[32];
        // compute the raw shared secret
        X25519.scalarMult(privateKey, 0, publicKey, 0, sharedSecret, 0);
        // encrypt the shared secret
        byte[] nonce = new byte[32];
        return Salsa.HSalsa20(nonce, sharedSecret, Salsa.SIGMA);
    }

    public byte[] decrypt(byte[] message) {
        byte[] ephemeralPublicKey = Arrays.copyOf(message, PUBLICKEY_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(message, PUBLICKEY_BYTES, message.length);
        byte[] nonce = createNonce(ephemeralPublicKey, publicKey);
        byte[] sharedSecret = computeSharedSecret(ephemeralPublicKey, privateKey);

        SecretBox box = new SecretBox(sharedSecret);
        return box.decrypt(nonce, ciphertext);
    }
}
