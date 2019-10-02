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

package io.crossbar.autobahn.demogallery.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import xbr.network.SimpleSeller;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectAndStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_test_suite_client:
                startActivity(new Intent(getApplicationContext(), TestSuiteClientActivity.class));
                break;
            case R.id.button_websocket_echo_client:
                startActivity(new Intent(getApplicationContext(), EchoClientActivity.class));
                break;
        }
    }

    private void connectAndStart() {
        Session wamp = new Session();
        wamp.addOnJoinListener((session, details) -> sell(session));
        wamp.addOnLeaveListener((session, details) -> {
            System.out.println(details.reason);
        });
        Client client = new Client(wamp, "ws://10.0.2.2:8080/ws", "realm1");
        client.connect().whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }

    private void sell(Session session) {
        SimpleSeller seller = new SimpleSeller(
                "0x3e5e9111ae8eb78fe1cc3bb8915d5d461f3ef9a9",
                "0xadd53f9a7e588d003326d1cbf9e4a43c061aadd9bc938c843a79e7b4fd2ad743"
        );
        seller.add(
                asBytes(UUID.randomUUID()),
                "io.crossbar.example",
                new BigInteger("35").multiply(new BigInteger("10").pow(18)),
                10
        );
        seller.start(session);

//        session.publish("io.crossbar.example", )
    }

    public static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
