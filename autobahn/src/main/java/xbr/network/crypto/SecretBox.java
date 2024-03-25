package xbr.network.crypto;

import java.util.Arrays;

import static io.xconn.cryptology.SecretBox.box;
import static io.xconn.cryptology.SecretBox.boxOpen;
import static io.xconn.cryptology.Util.checkLength;
import static io.xconn.cryptology.Util.generateRandomBytesArray;

import static xbr.network.Util.NONCE_SIZE;
import static xbr.network.Util.SECRET_KEY_LEN;

public class SecretBox {
    private final byte[] mKey;

    public SecretBox(byte[] key) {
        checkLength(key, SECRET_KEY_LEN);
        mKey = Arrays.copyOf(key, key.length);
    }

    public byte[] encrypt(byte[] message) {
        byte[] nonce = generateRandomBytesArray(NONCE_SIZE);
        return encrypt(nonce, message);
    }

    public byte[] encrypt(byte[] nonce, byte[] plaintext) {
        byte[] cipherWithoutNonce = box(nonce, plaintext, mKey);
        byte[] ciphertext = new byte[cipherWithoutNonce.length + NONCE_SIZE];
        System.arraycopy(nonce, 0, ciphertext, 0, nonce.length);
        System.arraycopy(cipherWithoutNonce, 0, ciphertext, nonce.length, cipherWithoutNonce.length);
        return ciphertext;
    }

    public byte[] decrypt(byte[] ciphertext) {
        byte[] nonce = Arrays.copyOfRange(ciphertext, 0, NONCE_SIZE);
        byte[] message = Arrays.copyOfRange(ciphertext, NONCE_SIZE, ciphertext.length);
        return boxOpen(nonce, message, mKey);
    }
}
