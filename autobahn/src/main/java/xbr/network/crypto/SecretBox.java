package xbr.network.crypto;

import static xbr.network.Util.NONCE_SIZE;
import static xbr.network.Util.SECRET_KEY_LEN;
import static xbr.network.Util.generateRandomBytesArray;

import org.bouncycastle.crypto.engines.XSalsa20Engine;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.security.MessageDigest;
import java.util.Arrays;

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
        checkLength(nonce, NONCE_SIZE);

        XSalsa20Engine xsalsa20 = new XSalsa20Engine();
        Poly1305 poly1305 = new Poly1305();

        xsalsa20.init(true, new ParametersWithIV(new KeyParameter(mKey), nonce));
        byte[] subKey = new byte[SECRET_KEY_LEN];
        xsalsa20.processBytes(subKey, 0, SECRET_KEY_LEN, subKey, 0);
        byte[] cipherWithoutNonce = new byte[plaintext.length + poly1305.getMacSize()];
        xsalsa20.processBytes(plaintext, 0, plaintext.length, cipherWithoutNonce, poly1305.getMacSize());

        // hash ciphertext and prepend mac to ciphertext
        poly1305.init(new KeyParameter(subKey));
        poly1305.update(cipherWithoutNonce, poly1305.getMacSize(), plaintext.length);
        poly1305.doFinal(cipherWithoutNonce, 0);

        byte[] ciphertext = new byte[cipherWithoutNonce.length +
                NONCE_SIZE];
        System.arraycopy(nonce, 0, ciphertext, 0, nonce.length);
        System.arraycopy(cipherWithoutNonce, 0, ciphertext, nonce.length,
                cipherWithoutNonce.length);
        return ciphertext;
    }

    public byte[] decrypt(byte[] ciphertext) {
        byte[] nonce = Arrays.copyOfRange(ciphertext, 0, NONCE_SIZE);
        byte[] message = Arrays.copyOfRange(ciphertext, NONCE_SIZE,
                ciphertext.length);
        return decrypt(nonce, message);
    }


    private byte[] decrypt(byte[] nonce, byte[] ciphertext) {
        checkLength(nonce, NONCE_SIZE);

        XSalsa20Engine xsalsa20 = new XSalsa20Engine();
        Poly1305 poly1305 = new Poly1305();

        xsalsa20.init(false, new ParametersWithIV(new KeyParameter(mKey), nonce));
        byte[] sk = new byte[SECRET_KEY_LEN];
        xsalsa20.processBytes(sk, 0, sk.length, sk, 0);

        // hash ciphertext
        poly1305.init(new KeyParameter(sk));
        int len = Math.max(ciphertext.length - poly1305.getMacSize(), 0);
        poly1305.update(ciphertext, poly1305.getMacSize(), len);
        byte[] calculatedMAC = new byte[poly1305.getMacSize()];
        poly1305.doFinal(calculatedMAC, 0);

        // extract mac
        final byte[] presentedMAC = new byte[poly1305.getMacSize()];
        System.arraycopy(
                ciphertext, 0, presentedMAC, 0, Math.min(ciphertext.length, poly1305.getMacSize()));

        if (!MessageDigest.isEqual(calculatedMAC, presentedMAC)) {
            throw new IllegalArgumentException("Invalid MAC");
        }

        byte[] plaintext = new byte[len];
        xsalsa20.processBytes(ciphertext, poly1305.getMacSize(), plaintext.length, plaintext, 0);
        return plaintext;
    }

    private void checkLength(byte[] data, int size) {
        if (data == null || data.length != size)
            throw new IllegalArgumentException("Invalid size: " + data.length);
    }
}