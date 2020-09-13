///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package xbr.network;

import org.json.JSONArray;
import org.json.JSONException;
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

    public static BigInteger toXBR(int xbr) {
        return BigInteger.valueOf(xbr).multiply(BigInteger.valueOf(10).pow(18));
    }

    public static BigInteger toXBR(byte[] xbr) {
        return new BigInteger(xbr);
    }

    public static BigInteger toXBR(Object xbr) {
        return new BigInteger((byte[]) xbr);
    }

    public static int toInt(BigInteger value) {
        BigInteger bi = new BigInteger("10").pow(18);
        return Integer.parseInt(value.divide(bi).toString());
    }

    private static JSONObject createEIP712Data(int chainId, String verifyingContract, int closeAt,
                                               String marketId, String channelId, int channelSeq,
                                               BigInteger balance, boolean isFinal)
            throws JSONException {

        JSONObject result = new JSONObject();

        JSONObject types = new JSONObject();
        JSONArray eip712Domain = new JSONArray(
                "[" +
                "{'name': 'name', 'type': 'string'}, " +
                "{'name': 'version', 'type': 'string'} " +
                "]"
        );
        JSONArray channelClose = new JSONArray(
                "[" +
                "{'name': 'chainId', 'type': 'uint256'}, " +
                "{'name': 'verifyingContract', 'type': 'address'}, " +
                "{'name': 'closeAt', 'type': 'uint256'}, " +
                "{'name': 'marketId', 'type': 'bytes16'}, " +
                "{'name': 'channelId', 'type': 'bytes16'}, " +
                "{'name': 'channelSeq', 'type': 'uint32'}, " +
                "{'name': 'balance', 'type': 'uint256'}," +
                "{'name': 'isFinal', 'type': 'bool'}" +
                "]"
        );
        types.put("EIP712Domain", eip712Domain);
        types.put("EIP712ChannelClose", channelClose);
        result.put("types", types);

        result.put("primaryType", "EIP712ChannelClose");

        JSONObject domain = new JSONObject();
        domain.put("name", "XBR");
        domain.put("version", "1");
        domain.put("verifyingContract", verifyingContract);

        JSONObject message = new JSONObject();
        message.put("chainId", chainId);
        message.put("verifyingContract", verifyingContract);
        message.put("closeAt", closeAt);
        message.put("marketId", marketId);
        message.put("channelId", channelId);
        message.put("channelSeq", channelSeq);
        message.put("balance", balance);
        message.put("isFinal", isFinal);

        result.put("domain", domain);
        result.put("message", message);

        return result;
    }

    static CompletableFuture<byte[]> signEIP712Data(ECKeyPair keyPair, int chainId,
                                                    String verifyingContract, int closeAt,
                                                    String marketId, String channelId,
                                                    int channelSeq, BigInteger balance,
                                                    boolean isFinal) {

        CompletableFuture<byte[]> future = new CompletableFuture<>();

        try {
            JSONObject data = createEIP712Data(chainId, verifyingContract, closeAt, marketId, channelId,
                    channelSeq, balance, isFinal);
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
        } catch (IOException | JSONException e) {
            future.completeExceptionally(e);
        }

        return future;
    }


    static CompletableFuture<String> recoverEIP712Signer(int chainId, String verifyingContract,
                                                         int closeAt, String marketId,
                                                         String channelId, int channelSeq,
                                                         BigInteger balance, boolean isFinal,
                                                         byte[] signature) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            JSONObject data = createEIP712Data(chainId, verifyingContract, closeAt, marketId, channelId,
                    channelSeq, balance, isFinal);
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
