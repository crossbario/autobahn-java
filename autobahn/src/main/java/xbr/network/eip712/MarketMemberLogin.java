package xbr.network.eip712;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.ECKeyPair;

import java.util.concurrent.CompletableFuture;

public class MarketMemberLogin {
    private static JSONObject createMarketMemberLogin(String member, String pubkey)
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
                "{'name': 'member', 'type': 'address'}, " +
                "{'name': 'client_pubkey', 'type': 'bytes32'} " +
                "]"
        );
        types.put("EIP712Domain", eip712Domain);
        types.put("EIP712MarketMemberLogin", channelClose);
        result.put("types", types);

        result.put("primaryType", "EIP712MarketMemberLogin");

        JSONObject domain = new JSONObject();
        domain.put("name", "XBR");
        domain.put("version", "1");

        // FIXME: the web3j apparently requires that value.
        domain.put("verifyingContract", member);

        JSONObject message = new JSONObject();
        message.put("member", member);
        message.put("client_pubkey", pubkey);

        result.put("domain", domain);
        result.put("message", message);

        return result;
    }

    public static CompletableFuture<byte[]> sign(ECKeyPair keyPair, String member, String pubkey) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        try {
            JSONObject data = createMarketMemberLogin(member, pubkey);
            Util.signTypedData(data, keyPair).whenComplete((bytes, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                } else {
                    future.complete(bytes);
                }
            });
        } catch (JSONException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public static CompletableFuture<String> recover(String member, String pubkey, byte[] sig) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            JSONObject data = createMarketMemberLogin(member, pubkey);
            Util.recoverySigner(
                    data, sig
            ).whenComplete((s, throwable) -> {
                if (throwable == null) {
                    future.complete(s);
                } else {
                    future.completeExceptionally(throwable);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}
