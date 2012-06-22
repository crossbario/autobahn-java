/******************************************************************************
 *
 *  Copyright 2011 Tavendo GmbH
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

package de.tavendo.autobahn.echoclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketConnectionHandler;

public class EchoClientActivity extends Activity {

   static final String TAG = "de.tavendo.autobahn.echo";
   private static final String PREFS_NAME = "AutobahnAndroidEcho";

   static EditText mHostname;
   static EditText mPort;
   static TextView mStatusline;
   static Button mStart;

   static EditText mMessage;
   static Button mSendMessage;

   private SharedPreferences mSettings;

   private void alert(String message) {
      Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
      toast.show();
   }

   private void loadPrefs() {

      mHostname.setText(mSettings.getString("hostname", ""));
      mPort.setText(mSettings.getString("port", "9000"));
   }

   private void savePrefs() {

      SharedPreferences.Editor editor = mSettings.edit();
      editor.putString("hostname", mHostname.getText().toString());
      editor.putString("port", mPort.getText().toString());
      editor.commit();
   }

   private void setButtonConnect() {
      mHostname.setEnabled(true);
      mPort.setEnabled(true);
      mStart.setText("Connect");
      mStart.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
            start();
         }
      });
   }

   private void setButtonDisconnect() {
      mHostname.setEnabled(false);
      mPort.setEnabled(false);
      mStart.setText("Disconnect");
      mStart.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
            mConnection.disconnect();
         }
      });
   }

   private final WebSocket mConnection = new WebSocketConnection();

   private void start() {

      final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();

      mStatusline.setText("Status: Connecting to " + wsuri + " ..");

      setButtonDisconnect();

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
            public void onTextMessage(String payload) {
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
         });
      } catch (WebSocketException e) {

         Log.d(TAG, e.toString());
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {

      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      mHostname = (EditText) findViewById(R.id.hostname);
      mPort = (EditText) findViewById(R.id.port);
      mStatusline = (TextView) findViewById(R.id.statusline);
      mStart = (Button) findViewById(R.id.start);
      mMessage = (EditText) findViewById(R.id.msg);
      mSendMessage = (Button) findViewById(R.id.sendMsg);

      mSettings = getSharedPreferences(PREFS_NAME, 0);
      loadPrefs();

      setButtonConnect();
      mSendMessage.setEnabled(false);
      mMessage.setEnabled(false);

      mSendMessage.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
            mConnection.sendTextMessage(mMessage.getText().toString());
         }
      });
   }

   @Override
   protected void onDestroy() {
       super.onDestroy();
       if (mConnection.isConnected()) {
          mConnection.disconnect();
       }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.main_menu, menu);
       return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.quit:
            finish();
            break;
         default:
            return super.onOptionsItemSelected(item);
      }
      return true;
   }
}
