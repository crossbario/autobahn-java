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
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleBuyer;
import xbr.network.Util;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connect();
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

    private void connect() {
        Session wampSession = new Session();
        wampSession.addOnJoinListener(this::startSelling);
        Client client = new Client(wampSession, "ws://10.0.2.2:8080/ws", "realm1");
        client.connect().whenComplete((exitInfo, throwable) -> {
            System.out.println("exit...");
        });
    }

    private void startSelling(Session session, SessionDetails details) {
        SimpleBuyer buyer = new SimpleBuyer(
                "0x3e5e9111ae8eb78fe1cc3bb8915d5d461f3ef9a9",
                "395df67f0c2d2d9fe1ad08d1bc8b6627011959b79c53d7dd6a3536a33ab8a4fd",
                Util.toXBR(100));
        buyer.start(session, details.authid).whenComplete((aLong, throwable) -> {
            System.out.println("Balance is: " + aLong);
            buyer.balance().whenComplete((stringObjectHashMap, throwable1) -> {
                System.out.println(stringObjectHashMap);
            });
        });
        System.out.println("read...");
    }
}
