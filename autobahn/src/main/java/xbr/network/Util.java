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
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

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

    private static JSONObject createEIP712Data(String verifyingAddr, byte[] channelAddr,
                                        int channelSeq, BigInteger balance, boolean isFinal)
            throws JSONException {
        JSONObject result = new JSONObject();

        JSONObject types = new JSONObject();
        JSONArray eip712Domain = new JSONArray(
                "[" +
                "{'name': 'name', 'type': 'string'}, " +
                "{'name': 'version', 'type': 'string'}, " +
                "{'name': 'chainId', 'type': 'uint256'}, " +
                "{'name': 'verifyingContract', 'type': 'address'}" +
                "]"
        );
        JSONArray channelClose = new JSONArray(
                "[" +
                "{'name': 'channel_adr', 'type': 'address'}, " +
                "{'name': 'channel_seq', 'type': 'uint32'}, " +
                "{'name': 'balance', 'type': 'uint256'}, " +
                "{'name': 'is_final', 'type': 'bool'}," +
                "]"
        );
        types.put("EIP712Domain", eip712Domain);
        types.put("ChannelClose", channelClose);
        result.put("types", types);

        result.put("primaryType", "ChannelClose");

        JSONObject domain = new JSONObject();
        domain.put("name", "XBR");
        domain.put("version", "1");
        domain.put("chainId", 1);
        domain.put("verifyingContract", verifyingAddr);
        result.put("domain", domain);

        JSONObject message = new JSONObject();
        message.put("channel_adr", Numeric.toHexString(channelAddr));
        message.put("channel_seq", channelSeq);
        message.put("balance", balance);
        message.put("is_final", isFinal);
        result.put("message", message);

        return result;
    }

    static byte[] signEIP712Data(ECKeyPair keyPair, byte[] channelAddr, int channelSeq,
                                 BigInteger balance, boolean isFinal)
            throws IOException, JSONException {

        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
        JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
                isFinal);
        StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
        byte[] message = encoder.hashStructuredData();
        Sign.SignatureData signed = Sign.signMessage(message, keyPair, false);

        byte[] r = signed.getR();
        byte[] s = signed.getS();
        byte[] result = new byte[65];
        System.arraycopy(r, 0, result, 0, r.length);
        System.arraycopy(s, 0, result, r.length, s.length);
        result[64] = signed.getV();

        return result;
    }

    static String recoverEIP712Signer(byte[] channelAddr, int channelSeq, BigInteger balance,
                                      boolean isFinal, byte[] signature) {
        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";

        try {
            JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
                    isFinal);
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
                return "0x" + Keys.getAddress(publicKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
