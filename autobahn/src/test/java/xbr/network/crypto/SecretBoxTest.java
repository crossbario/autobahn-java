package xbr.network.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static io.xconn.cryptology.Util.generateRandomBytesArray;

import static xbr.network.Util.NONCE_SIZE;

import org.junit.Test;

public class SecretBoxTest {

    @Test
    public void testConstructor() {
        // test with valid key
        new SecretBox(new byte[32]);

        // test with invalid key
        assertThrows(IllegalArgumentException.class, () -> new SecretBox(new byte[16]));

        // test with null key
        assertThrows(NullPointerException.class, () -> new SecretBox(null));
    }

    @Test
    public void testEncryptAndDecrypt() {
        SecretBox secretBox = new SecretBox(new byte[32]);
        byte[] message = "Hello, World!".getBytes();
        byte[] encrypted = secretBox.encrypt(message);
        byte[] decrypted = secretBox.decrypt(encrypted);
        assertArrayEquals(message, decrypted);
    }

    @Test
    public void testEncryptAndDecryptWithNonce() {
        SecretBox secretBox = new SecretBox(new byte[32]);
        byte[] nonce = generateRandomBytesArray(NONCE_SIZE);
        byte[] message = "Hello, World!".getBytes();
        byte[] encrypted = secretBox.encrypt(nonce, message);
        byte[] decrypted = secretBox.decrypt(encrypted);
        assertArrayEquals(message, decrypted);
    }

    @Test
    public void testEncryptAndDecryptWithInvalidMAC() {
        SecretBox secretBox = new SecretBox(new byte[32]);
        byte[] message = "Hello, World!".getBytes();
        byte[] encrypted = secretBox.encrypt(message);
        encrypted[encrypted.length - 1] ^= 0xFF; // Modify last byte
        assertThrows(IllegalArgumentException.class, () -> secretBox.decrypt(encrypted));
    }

    @Test
    public void testEncryptAndDecryptWithInvalidNonce() {
        SecretBox secretBox = new SecretBox(new byte[32]);
        byte[] message = "Hello, World!".getBytes();
        byte[] encrypted = secretBox.encrypt(message);
        encrypted[0] ^= 0xFF; // Modify first byte
        assertThrows(IllegalArgumentException.class, () -> secretBox.decrypt(encrypted));
    }

    @Test
    public void testEncryptAndDecryptWithModifiedCiphertext() {
        byte[] key = new byte[32];
        SecretBox secretBox = new SecretBox(key);
        byte[] message = "Hello, World!".getBytes();
        byte[] encrypted = secretBox.encrypt(message);
        encrypted[NONCE_SIZE + 1] ^= 0xFF; // Modify the byte next to nonce
        assertThrows(IllegalArgumentException.class, () -> secretBox.decrypt(encrypted));
    }

}
