/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package io.crossbar.autobahn.demogallery;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.crossbar.autobahn.WebSocket;
import io.crossbar.autobahn.WebSocketConnection;
import io.crossbar.autobahn.WebSocketConnectionHandler;
import io.crossbar.autobahn.WebSocketException;
import io.crossbar.autobahn.WebSocketOptions;

//import dalvik.system.VMRuntime;

public class TestSuiteClientActivity extends Activity {

    static final String TAG = "io.crossbar.autobahn.demogallery";

    private static final String PREFS_NAME = "AutobahnAndroidTestsuiteClient";

    private SharedPreferences mSettings;

    static EditText mWsUri;
    static EditText mAgent;
    static TextView mStatusline;
    static Button mStart;

    private void loadPrefs() {
        mWsUri.setText(mSettings.getString("wsuri", "ws://192.168.1.12:9001"));
        mAgent.setText(mSettings.getString("agent", "AutobahnAndroid"));
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("wsuri", mWsUri.getText().toString());
        editor.putString("agent", mAgent.getText().toString());
        editor.commit();
    }

    int currCase = 0;
    int lastCase = 0;

    private WebSocket sess = new WebSocketConnection();

    private void next() {

        try {
            if (currCase == 0) {

                sess.connect(mWsUri.getText() + "/getCaseCount",
                        new WebSocketConnectionHandler() {

                            @Override
                            public void onOpen() {
                                savePrefs();
                            }

                            @Override
                            public void onTextMessage(String payload) {
                                System.out.println(payload);
                                lastCase = Integer.parseInt(payload);
                            }

                            @Override
                            public void onRawTextMessage(byte[] payload) {
                                System.out.println("RTM");
                            }

                            @Override
                            public void onBinaryMessage(byte[] payload) {
                                System.out.println("BM");
                            }

                            @Override
                            public void onClose(int code, String reason) {
                                mStatusline.setText("Ok, will run " + lastCase + " cases.");
                                currCase += 1;
                                next();
                            }
                        });

            } else {
                if (currCase <= lastCase) {

                    WebSocketOptions options = new WebSocketOptions();
                    options.setReceiveTextMessagesRaw(true);
                    //options.setValidateIncomingUtf8(false);
                    //options.setMaskClientFrames(false);
                    options.setMaxMessagePayloadSize(4 * 1024 * 1024);
                    options.setMaxFramePayloadSize(4 * 1024 * 1024);
                    //options.setTcpNoDelay(false);

                    sess.connect(mWsUri.getText() + "/runCase?case=" + currCase + "&agent=" + mAgent.getText(),
                            new WebSocketConnectionHandler() {

                                @Override
                                public void onRawTextMessage(byte[] payload) {
                                    sess.sendRawTextMessage(payload);
                                }

                                @Override
                                public void onBinaryMessage(byte[] payload) {
                                    sess.sendBinaryMessage(payload);
                                }

                                @Override
                                public void onOpen() {
                                    mStatusline.setText("Test case " + currCase + "/" + lastCase + " started ..");
                                }

                                @Override
                                public void onClose(int code, String reason) {
                                    mStatusline.setText("Test case " + currCase + "/" + lastCase + " finished.");
                                    currCase += 1;
                                    next();
                                }
                            }, options);
                } else {
                    sess.connect(mWsUri.getText() + "/updateReports?agent=" + mAgent.getText(),
                            new WebSocketConnectionHandler() {

                                @Override
                                public void onOpen() {
                                    mStatusline.setText("Updating test reports ..");
                                }

                                @Override
                                public void onClose(int code, String reason) {
                                    mStatusline.setText("Test reports updated. Finished.");
                                    mStart.setEnabled(true);
                                }
                            });
                }
            }
        } catch (WebSocketException e) {

            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        //VMRuntime heap = VMRuntime.getRuntime();
        //heap.setMinimumHeapSize(128 * 1024 * 1024);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_suite_client);

        mWsUri = (EditText) findViewById(R.id.wsuri);
        mAgent = (EditText) findViewById(R.id.agent);
        mStatusline = (TextView) findViewById(R.id.statusline);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        mStart = (Button) findViewById(R.id.start);
        mStart.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {
                mStart.setEnabled(false);
                currCase = 0;
                next();
            }

        });
    }

}
