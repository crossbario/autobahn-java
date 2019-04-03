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

import java.util.concurrent.Executors;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import xbr.network.SimpleBuyer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        test();
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

    private void test() {
        Session session = new Session(Executors.newSingleThreadExecutor());
        session.addOnJoinListener((session1, details) -> {
            System.out.println("CONNECTED");
            SimpleBuyer buyer = new SimpleBuyer(
                    "395df67f0c2d2d9fe1ad08d1bc8b6627011959b79c53d7dd6a3536a33ab8a4fd", 0);
            buyer.start(session1, 900).whenComplete((integer, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                } else {
                    System.out.println("GOT          + " + integer);
                }
            });

        });
        Client client = new Client(session, "ws://192.168.0.9:8080/ws", "realm1");
        client.connect().whenComplete((exitInfo, throwable) -> {
            System.out.println("EXIT");
        });
    }
}
