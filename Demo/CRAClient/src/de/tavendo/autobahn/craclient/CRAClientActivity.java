/******************************************************************************
 *
 *  Copyright 2012 Alejandro Hernandez
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package de.tavendo.autobahn.craclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.tavendo.autobahn.Wamp;
import de.tavendo.autobahn.WampCra;
import de.tavendo.autobahn.WampCraConnection;

public class CRAClientActivity extends Activity {

    static final String TAG = "de.tavendo.autobahn.cra";
    private static final String PREFS_NAME = "AutobahnAndroidCRA";

    private SharedPreferences mSettings;

    private static EditText mHostname;
    private static EditText mPort;
    private static EditText mPath;
    private static TextView mStatusline;
    private static Button mStart;

    private void alert(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadPrefs() {

        mHostname.setText(mSettings.getString("hostname", "10.0.2.2"));
        mPort.setText(mSettings.getString("port", "9000"));
        mPath.setText(mSettings.getString("path", ""));
    }

    private void savePrefs() {

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("hostname", mHostname.getText().toString());
        editor.putString("port", mPort.getText().toString());
        editor.putString("path", mPath.getText().toString());
        editor.commit();
    }

    private void setButtonConnect() {
        mStart.setText("Connect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                test();
            }
        });
    }

    private void setButtonDisconnect() {
        mStart.setText("Disconnect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mConnection.disconnect();
            }
        });
    }

    private final WampCra mConnection = new WampCraConnection();

    private void test() {

        final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText() + "/" + mPath.getText();

        mStatusline.setText("Connecting to\n" + wsuri + " ..");

        setButtonDisconnect();

        // we establish a connection by giving the WebSockets URL of the server
        // and the handler for open/close events
        mConnection.connect(wsuri, new WampCra.ConnectionHandler() {

            @Override
            public void onOpen() {

                // The connection was successfully established. we set the
                // status
                // and save the host/port as Android application preference for
                // next time.
                mStatusline.setText("Connected to\n" + wsuri);
                savePrefs();

                mConnection.authenticate(new WampCra.AuthHandler() {

                    @Override
                    public void onAuthSuccess(Object permissions) {
                        mStatusline.setText("Authenticated");

                        testRpc();
                        testPubSub();
                    }

                    @Override
                    public void onAuthError(String errorUri, String errorDesc) {
                        // TODO Auto-generated method stub

                    }

                }, "foobar", "secret");

            }

            @Override
            public void onClose(int code, String reason) {

                // The connection was closed. Set the status line, show a
                // message box,
                // and set the button to allow to connect again.
                mStatusline.setText("Connection closed.");
                alert(reason);
                setButtonConnect();
            }
        });
    }

    private void testRpc() {
       
       final String echoUri = "http://example.com/procedures/hello";
       //final String echoUri = "http://api.wamp.ws/procedure#echo";

        mConnection.call(echoUri, String.class, new WampCra.CallHandler() {

            @Override
            public void onResult(Object result) {
                String res = (String) result;
                alert(echoUri + ": result = " + res);
            }

            @Override
            public void onError(String errorId, String errorInfo) {
                alert(echoUri + ": error = " + errorInfo);
            }
        }, "Foobar");
    }

    private void testPubSub() {
        mConnection.subscribe("http://example.com/topics/mytopic1", String.class, new Wamp.EventHandler() {

            @Override
            public void onEvent(String topicUri, Object event) {

                // when we get an event, we safely can cast to the type we
                // specified previously
                String evt = (String) event;

                alert("Event received : " + evt);
            }
        });
        
        mConnection.publish("http://example.com/topics/mytopic1","Hello, world!");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHostname = (EditText) findViewById(R.id.hostname);
        mPort = (EditText) findViewById(R.id.port);
        mPath = (EditText) findViewById(R.id.path);
        mStatusline = (TextView) findViewById(R.id.statusline);
        mStart = (Button) findViewById(R.id.start);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        setButtonConnect();
    }
}