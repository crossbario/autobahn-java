package xbr.network.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;

public class SealedBoxTest {

    private static byte[] publicKey;
    private static byte[] privateKey;

    @Before
    public void setUp() {
        publicKey = Hex.decode("e146721761cf7378cb2e007adc1a51b70fa40abfb87652c645d8e86be19c2b1e");
        privateKey = Hex.decode("3817e2630237d569188a02a06354d9e9f61ee9cdd0cc8b5388c56013b7b5654a");
    }
    @Test
    public void testEncryptDecrypt() {
        SealedBox sealedBox = new SealedBox(publicKey, privateKey);

        String message = "Hello, world!";
        byte[] encrypted = sealedBox.encrypt(message.getBytes());
        byte[] decrypted = sealedBox.decrypt(encrypted);

        assertArrayEquals(message.getBytes(), decrypted);
    }

    @Test
    public void testNullPublicKey() {
        assertThrows(IllegalArgumentException.class, () -> new SealedBox(null));
    }

    @Test
    public void testNullPrivateKey() {
        byte[] publicKey = Hex.decode("1eb32ea638c250f7b781b7a0d29d0c1b3456d7a3428ff9c7a4a64d75db709709");

        assertThrows(IllegalArgumentException.class, () -> new SealedBox(publicKey, null));
    }

}
