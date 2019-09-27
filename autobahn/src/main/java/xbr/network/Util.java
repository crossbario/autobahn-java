package xbr.network;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Util {

    private String createEIP712Data(byte[] verifyingAddr, byte[] channelAddr, int channelSeq,
                                    int balance, boolean isFinal) throws JSONException {
        Map<String, Object> result = new HashMap<>();

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

        Map<String, Object> domain = new HashMap<>();
        domain.put("name", "XBR");
        domain.put("version", "1");
        domain.put("chainId", 1);
        domain.put("verifyingContract", verifyingAddr);

        Map<String, Object> message = new HashMap<>();
        message.put("channel_adr", channelAddr);
        message.put("channel_seq", channelSeq);
        message.put("balance", balance);
        message.put("is_final", isFinal);

        result.put("types", types);
        result.put("primaryType", "ChannelClose");
        result.put("domain", domain);
        result.put("message", message);

        return result.toString();
    }

    void signEIP712Data(byte[] ethPrivKey, byte[] channelAddr, int channelReq, int balance,
                        boolean isFinal) throws IOException {
        String verifyingAddr = "0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B";
//        StructuredDataEncoder encoder = new StructuredDataEncoder();
//        encoder.

    }
}
