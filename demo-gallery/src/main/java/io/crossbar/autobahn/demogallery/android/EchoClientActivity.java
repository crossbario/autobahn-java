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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IWebSocket;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;


public class EchoClientActivity extends AppCompatActivity {

    private static final String TAG = "io.crossbar.autobahn.echo";
    private static final String PREFS_NAME = "AutobahnAndroidEcho";

    private EditText mHostname;
    private EditText mPort;
    private TextView mStatusline;
    private Button mStart;
    private EditText mMessage;
    private Button mSendMessage;
    private SharedPreferences mSettings;

    private void alert(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    private void loadPrefs() {
        mHostname.setText(mSettings.getString("hostname", "192.168.1.3"));
        mPort.setText(mSettings.getString("port", "9000"));
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("hostname", mHostname.getText().toString());
        editor.putString("port", mPort.getText().toString());
        editor.apply();
    }

    private void setButtonConnect() {
        mHostname.setEnabled(true);
        mPort.setEnabled(true);
        mStart.setText("Connect");
        mStart.setOnClickListener(v -> start());
    }

    private void setButtonDisconnect() {
        mHostname.setEnabled(false);
        mPort.setEnabled(false);
        mStart.setText("Disconnect");
        mStart.setOnClickListener(v -> mConnection.sendClose());
    }

    private final IWebSocket mConnection = new WebSocketConnection();

    private void start() {

        String hostname = mHostname.getText().toString();
        if (!hostname.startsWith("ws://") && !hostname.startsWith("wss://")) {
            hostname = "ws://" + hostname;
        }
        String port = mPort.getText().toString();

        String wsuri;
        if (!port.isEmpty()) {
            wsuri = hostname + ":" + port;
        } else {
            wsuri = hostname;
        }

        mStatusline.setText("Status: Connecting to " + wsuri + " ..");

        setButtonDisconnect();
        WebSocketOptions connectOptions = new WebSocketOptions();
        connectOptions.setReconnectInterval(5000);

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    mStatusline.setText("Status: Connected to " + wsuri);
                    savePrefs();
                    mSendMessage.setEnabled(true);
                    mMessage.setEnabled(true);
                }

                @Override
                public void onMessage(String payload) {
                    alert("Got echo: " + payload);
                }

                @Override
                public void onClose(int code, String reason) {
                    alert("Connection lost.");
                    mStatusline.setText("Status: Ready.");
                    setButtonConnect();
                    mSendMessage.setEnabled(false);
                    mMessage.setEnabled(false);
                }
            }, connectOptions);
        } catch (WebSocketException e) {
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_websocket_echo_client);

        mHostname = findViewById(R.id.hostname);
        mPort = findViewById(R.id.port);
        mStatusline = findViewById(R.id.statusline);
        mStart = findViewById(R.id.start);
        mMessage = findViewById(R.id.msg);
        mSendMessage = findViewById(R.id.sendMsg);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        setButtonConnect();
        mSendMessage.setEnabled(false);
        mMessage.setEnabled(false);

        mSendMessage.setOnClickListener(v -> mConnection.sendMessage(mMessage.getText().toString()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection.isConnected()) {
            mConnection.sendClose();
        }
    }
}
