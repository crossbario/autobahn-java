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
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebSocketConnection connection = new WebSocketConnection();
        try {
            connection.connect("wss://echo.websocket.org", new WebSocketConnectionHandler() {
                @Override
                public void onConnect(ConnectionResponse response) {
                    System.out.println("Connected to server");
                }

                @Override
                public void onOpen() {
                    connection.sendMessage("Echo with Autobahn");
                }

                @Override
                public void onClose(int code, String reason) {
                    System.out.println("Connection closed");
                }

                @Override
                public void onMessage(String payload) {
                    System.out.println("Received message: " + payload);
                    connection.sendMessage(payload);
                }
            });
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
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
}
