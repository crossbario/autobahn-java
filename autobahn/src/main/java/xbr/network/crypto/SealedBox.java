package xbr.network.crypto;

import static io.xconn.cryptology.SealedBox.seal;
import static io.xconn.cryptology.SealedBox.sealOpen;

public class SealedBox {

    private static final int MAC_BYTES = 16;
    private static int PUBLICKEY_BYTES = 32;
    private static final int SEAL_BYTES = PUBLICKEY_BYTES + MAC_BYTES;

    private static final byte[] HSALSA20_SEED = new byte[16];
    private byte[] publicKey;
    private byte[] privateKey;

    public SealedBox(byte[] publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key must not be null");
        }
        this.publicKey = publicKey;
        this.privateKey = null;
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

    public byte[] encrypt(byte[] message) {
        return seal(message, publicKey);
    }

    public byte[] decrypt(byte[] message) {
        return sealOpen(message, privateKey);
    }
}
