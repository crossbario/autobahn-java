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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;


public class TestSuiteClientActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String PREFS_NAME = "AutobahnAndroidTestsuiteClient";

    private SharedPreferences mSettings;

    private EditText mWsUri;
    private EditText mAgent;
    private TextView mStatusLine;
    private Button mStart;

    private int currentCase;
    private int lastCase;

    private WebSocketOptions mOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_suite_client);

        mWsUri = (EditText) findViewById(R.id.wsuri);
        mAgent = (EditText) findViewById(R.id.agent);
        mStatusLine = (TextView) findViewById(R.id.statusline);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        mStart = (Button) findViewById(R.id.start);
        mStart.setOnClickListener(this);

        mOptions = new WebSocketOptions();
        mOptions.setReceiveTextMessagesRaw(true);
        mOptions.setMaxMessagePayloadSize(16 * 1024 * 1024);
        mOptions.setMaxFramePayloadSize(16 * 1024 * 1024);
        mOptions.setAutoPingInterval(0);
    }

    private void loadPrefs() {
        mWsUri.setText(mSettings.getString("wsuri", "ws://192.168.1.3:9001"));
        mAgent.setText(mSettings.getString("agent", "AutobahnAndroid"));
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("wsuri", mWsUri.getText().toString());
        editor.putString("agent", mAgent.getText().toString());
        editor.apply();
    }

    private void updateText(final TextView textView, final String text) {
        runOnUiThread(() -> textView.setText(text));
    }

    private void runTest() throws WebSocketException {
        final WebSocketConnection webSocket = new WebSocketConnection();
        webSocket.connect(mWsUri.getText() + "/runCase?case=" + currentCase + "&agent=" + mAgent.getText(),
                new WebSocketConnectionHandler() {

                    @Override
                    public void onMessage(String payload) {
                        webSocket.sendMessage(payload);
                    }

                    @Override
                    public void onMessage(byte[] payload, boolean isBinary) {
                        webSocket.sendMessage(payload, isBinary);
                    }

                    @Override
                    public void onOpen() {
                        updateText(mStatusLine, "Test case " + currentCase + "/" + lastCase + " started ..");
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        mStatusLine.setText("Test case " + currentCase + "/" + lastCase + " finished.");
                        currentCase += 1;
                        processNext();
                    }
                }, mOptions);
    }

    private void updateReport() throws WebSocketException {
        WebSocketConnection webSocket = new WebSocketConnection();
        webSocket.connect(mWsUri.getText() + "/updateReports?agent=" + mAgent.getText(),
                new WebSocketConnectionHandler() {
                    @Override
                    public void onOpen() {
                        mStatusLine.setText("Updating test reports ..");
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        mStatusLine.setText("Test reports updated. Finished.");
                        mStart.setEnabled(true);
                    }
                });
    }

    private void queryCaseCount() throws WebSocketException {
        final WebSocketConnection webSocket = new WebSocketConnection();
        webSocket.connect(mWsUri.getText() + "/getCaseCount", new WebSocketConnectionHandler() {

                    @Override
                    public void onOpen() {
                        savePrefs();
                    }

                    @Override
                    public void onMessage(String payload) {
                        lastCase = Integer.parseInt(payload);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        mStatusLine.setText("Ok, will run " + lastCase + " cases.");
                        currentCase += 1;
                        processNext();
                    }
                });
    }

    private void processNext() {
        try {
            if (currentCase == 0) {
                queryCaseCount();
            } else if (currentCase <= lastCase) {
                runTest();
            } else {
                updateReport();
            }
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start) {
            mStart.setEnabled(false);
            currentCase = 0;
            processNext();
        }
    }
}
