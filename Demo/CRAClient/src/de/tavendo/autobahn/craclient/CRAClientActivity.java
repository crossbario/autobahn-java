package de.tavendo.autobahn.craclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
    private static TextView mStatusline;
    private static Button mStart;

    private void alert(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadPrefs() {

        mHostname.setText(mSettings.getString("hostname", "10.0.2.2"));
        mPort.setText(mSettings.getString("port", "9000"));
    }

    private void savePrefs() {

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("hostname", mHostname.getText().toString());
        editor.putString("port", mPort.getText().toString());
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

        final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();

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

        mConnection.call("http://example.com/procedures/hello", String.class, new WampCra.CallHandler() {

            @Override
            public void onResult(Object result) {
                String res = (String) result;
                alert("http://example.com/procedures/hello result = " + res);
            }

            @Override
            public void onError(String errorId, String errorInfo) {
                alert("http://example.com/procedures/hello RPC error - " + errorInfo);
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
        mStatusline = (TextView) findViewById(R.id.statusline);
        mStart = (Button) findViewById(R.id.start);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        setButtonConnect();
    }
}