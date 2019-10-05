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
import java.security.SignatureException;
import java.util.Arrays;

public class Util {

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

    static byte[] signEIP712Data(byte[] ethPrivKey, byte[] channelAddr, int channelSeq, BigInteger balance,
                          boolean isFinal) throws IOException, JSONException, SignatureException {
        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
        JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
                isFinal);
        StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
        byte[] message = encoder.hashStructuredData();
        Sign.SignatureData signed = Sign.signMessage(message, ECKeyPair.create(ethPrivKey));
        return Sign.signedMessageToKey(message, signed).toByteArray();
    }

    static String recoverEIP712Signer(byte[] channelAddr, int channelSeq, BigInteger balance,
                                      boolean isFinal, byte[] signature)
            throws JSONException, IOException {
        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
        JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
                isFinal);

        try {
            System.out.println(signature.length);
            StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
            ECDSASignature sig = new ECDSASignature(
                    new BigInteger(Arrays.copyOfRange(signature, 0, 31)),
                    new BigInteger(Arrays.copyOfRange(signature, 32, 63))
            );
            byte[] message = encoder.hashStructuredData();
            for (int recID = 0; recID <= 3; recID++) {
                BigInteger recovered = Sign.recoverFromSignature(recID, sig, message);
                if (recovered != null) {
                    String rawAddress = Numeric.toHexStringWithPrefix(recovered);
                    System.out.println(rawAddress);
                    return Keys.toChecksumAddress(rawAddress.substring(rawAddress.length() - 40));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
