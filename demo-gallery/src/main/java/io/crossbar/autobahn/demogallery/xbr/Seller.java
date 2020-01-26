package io.crossbar.autobahn.demogallery.xbr;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleSeller;

public class Seller {

    public void sellBasic() {
        Session session = new Session();
        session.addOnJoinListener(this::onJoin);
        Client client = new Client(session, "ws://10.0.2.2:8080/ws", "realm1");
        client.connect().whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");
        SimpleSeller seller = new SimpleSeller(
                "0x3e5e9111ae8eb78fe1cc3bb8915d5d461f3ef9a9",
                "0xadd53f9a7e588d003326d1cbf9e4a43c061aadd9bc938c843a79e7b4fd2ad743"
        );
        // Ugly, find a "nicer" way of doing things... (35 XBR)
        BigInteger price = BigInteger.valueOf(35).multiply(BigInteger.valueOf(10).pow(18));
        int intervalSeconds = 10;
        String topic = "io.crossbar.example";
        // This is a random API key of 16 bytes
        byte[] apiID = new byte[16];
        seller.add(apiID, topic, price, intervalSeconds);

        seller.start(session).whenComplete((integer, throwable) -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "crossbario");
            payload.put("country", "DE");
            payload.put("level", "Crossbar is super cool!");
            try {
                Map<String, Object> enc = seller.wrap(apiID, topic, payload);
                List<Object> args = new ArrayList<>();
                args.add(enc.get("id"));
                args.add(enc.get("serializer"));
                args.add(enc.get("ciphertext"));
                session.publish(topic, args, null, new PublishOptions(true, true));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
