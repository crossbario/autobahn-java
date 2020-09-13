package xbr.network.eip712;

import org.json.JSONObject;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class Util {
    static CompletableFuture<byte[]> signTypedData(JSONObject data, ECKeyPair keyPair) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        try {
            System.out.println(data.toString());
            StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
            byte[] message = encoder.hashStructuredData();

            Sign.SignatureData signed = Sign.signMessage(message, keyPair, false);

            byte[] r = signed.getR();
            byte[] s = signed.getS();
            byte[] result = new byte[65];
            System.arraycopy(r, 0, result, 0, r.length);
            System.arraycopy(s, 0, result, r.length, s.length);
            result[64] = signed.getV()[0];

            future.complete(result);
        } catch (IOException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    static CompletableFuture<String> recoverySigner(JSONObject data, byte[] signature) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
            byte[] message = encoder.hashStructuredData();
            byte v = signature[64];
            if (v < 27) {
                v += 27;
            }
            byte[] r = Arrays.copyOfRange(signature, 0, 32);
            byte[] s = Arrays.copyOfRange(signature, 32, 64);

            Sign.SignatureData sd = new Sign.SignatureData(v, r, s);

            int recID = v - 27;
            BigInteger publicKey =
                    Sign.recoverFromSignature(
                            (byte) recID,
                            new ECDSASignature(
                                    new BigInteger(1, sd.getR()), new BigInteger(1, sd.getS())),
                            message);

            if (publicKey != null) {
                future.complete("0x" + Keys.getAddress(publicKey));
            } else {
                future.complete(null);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}
