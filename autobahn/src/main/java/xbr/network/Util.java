package xbr.network;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.io.IOException;
import java.security.SignatureException;

public class Util {

    private JSONObject createEIP712Data(String verifyingAddr, byte[] channelAddr,
                                        int channelSeq, int balance, boolean isFinal)
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
        message.put("channel_adr", channelAddr);
        message.put("channel_seq", channelSeq);
        message.put("balance", balance);
        message.put("is_final", isFinal);
        result.put("message", message);

        return result;
    }

    byte[] signEIP712Data(byte[] ethPrivKey, byte[] channelAddr, int channelSeq, int balance,
                          boolean isFinal) throws IOException, JSONException, SignatureException {
        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
        JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
                isFinal);
        StructuredDataEncoder encoder = new StructuredDataEncoder(data.toString());
        byte[] message = encoder.hashStructuredData();
        Sign.SignatureData signed = Sign.signMessage(message, ECKeyPair.create(ethPrivKey));
        return Sign.signedMessageToKey(message, signed).toByteArray();

    }

//    byte[] recoveryEIP712Signer(byte[] channelAddr, int channelSeq, int balance, boolean isFinal,
//                                byte[] signature) throws JSONException {
//        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
//        JSONObject data = createEIP712Data(verifyingAddr, channelAddr, channelSeq, balance,
//                isFinal);
//        Sign.SignatureData signatureData = Sign.SignatureData()
//        Sign.recoverFromSignature()
//    }
}
