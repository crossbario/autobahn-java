package xbr.network.crypto;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.libsodium.jni.encoders.Encoder;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.NONCE_BYTES;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.crypto.Util.isValid;

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

    private byte[] createNonce(byte[] ephemeralPublicKey, byte[] recipientPublicKey) {
        Blake2bDigest blake2b = new Blake2bDigest(NONCE_BYTES * 8);
        byte[] nonce = new byte[blake2b.getDigestSize()];

        blake2b.update(ephemeralPublicKey, 0, ephemeralPublicKey.length);
        blake2b.update(recipientPublicKey, 0, recipientPublicKey.length);

        blake2b.doFinal(nonce, 0);

        return nonce;
    }

    public byte[] decrypt(byte[] ciphertext) {
        byte[] message = new byte[ciphertext.length - SEAL_BYTES];
        isValid(sodium().crypto_box_seal_open(
                        message, ciphertext, ciphertext.length, publicKey, privateKey),
                "Decryption failed. Ciphertext failed verification");
        return message;
    }
}
