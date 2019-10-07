package xbr.network;

import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.Util;
import org.libsodium.jni.encoders.Encoder;

import java.util.Arrays;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.BOXZERO_BYTES;
import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES;
import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;
import static org.libsodium.jni.SodiumConstants.ZERO_BYTES;
import static org.libsodium.jni.crypto.Util.checkLength;
import static org.libsodium.jni.crypto.Util.isValid;
import static org.libsodium.jni.crypto.Util.removeZeros;


public class SecretBox {

    private byte[] mKey;
    private Encoder mEncoder;

    public SecretBox(byte[] key) {
        checkLength(key, XSALSA20_POLY1305_SECRETBOX_KEYBYTES);
        mEncoder = Encoder.RAW;
        mKey = key;
    }

    public byte[] encrypt(byte[] message) {
        byte[] nonce = new Random().randomBytes(XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);
        return encrypt(nonce, message);
    }

    public byte[] encrypt(byte[] nonce, byte[] message) {
        checkLength(nonce, XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);
        byte[] msg = org.libsodium.jni.crypto.Util.prependZeros(ZERO_BYTES, message);
        byte[] ct = org.libsodium.jni.crypto.Util.zeros(msg.length);
        isValid(sodium().crypto_secretbox_xsalsa20poly1305(ct, msg, msg.length,
                nonce, mKey), "Encryption failed");
        byte[] cipherWithoutNonce = removeZeros(BOXZERO_BYTES, ct);
        byte[] ciphertext = new byte[cipherWithoutNonce.length +
                XSALSA20_POLY1305_SECRETBOX_NONCEBYTES];
        System.arraycopy(nonce, 0, ciphertext, 0, nonce.length);
        System.arraycopy(cipherWithoutNonce, 0, ciphertext, nonce.length,
                cipherWithoutNonce.length);
        return ciphertext;
    }

    public byte[] decrypt(byte[] ciphertext) {
        byte[] nonce = Arrays.copyOfRange(ciphertext, 0, XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);
        byte[] message = Arrays.copyOfRange(ciphertext, XSALSA20_POLY1305_SECRETBOX_NONCEBYTES,
                ciphertext.length);
        return decrypt(nonce, message);
    }

    public byte[] decrypt(byte[] nonce, byte[] ciphertext) {
        checkLength(nonce, XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);
        byte[] ct = org.libsodium.jni.crypto.Util.prependZeros(BOXZERO_BYTES, ciphertext);
        byte[] message = Util.zeros(ct.length);
        isValid(sodium().crypto_secretbox_xsalsa20poly1305_open(message, ct,
                ct.length, nonce, mKey), "Decryption failed. Ciphertext failed verification");
        return removeZeros(ZERO_BYTES, message);
    }
}
