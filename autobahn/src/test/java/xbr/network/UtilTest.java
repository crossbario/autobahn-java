package xbr.network;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.crossbar.autobahn.utils.Pair;

public class UtilTest {

    @Test
    public void testGenerateRandomBytesArray() {
        int size = 32;
        byte[] randomBytes = Util.generateRandomBytesArray(size);

        assertNotNull(randomBytes);
        assertEquals(size, randomBytes.length);
    }

    @Test
    public void testGenerateKeyPair() {
        Pair<byte[], byte[]> keyPair = Util.generateX25519KeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.first);
        assertNotNull(keyPair.second);

        assertEquals(32, keyPair.first.length);
        assertEquals(32, keyPair.second.length);
    }
}
