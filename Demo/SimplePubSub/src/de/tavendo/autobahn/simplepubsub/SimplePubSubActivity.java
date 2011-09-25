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

package de.tavendo.autobahn.simplepubsub;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.tavendo.autobahn.Autobahn;
import de.tavendo.autobahn.AutobahnConnection;

public class SimplePubSubActivity extends Activity {

   static final String TAG = "de.tavendo.autobahn.simplerpc";

   final AutobahnConnection sess = new AutobahnConnection();

   static EditText mHostname;
   static EditText mPort;
   static TextView mStatusline;
   static Button mStart;

   static private class Simple {

      public int num;
      public String name;
      public String value;

      @Override
      public String toString() {
         return "{name: " + name + ", value: " + value + ", num: " + num + "}";
      }
   }

   private void alert(String message) {
      Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
   }

   private void test() {

      final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();

      mStatusline.setText("Connecting to\n" + wsuri + " ..");

      sess.connect(wsuri, new Autobahn.OnSession() {

         @Override
         public void onOpen() {

            mStatusline.setText("Connected to\n" + wsuri);

            sess.prefix("event", "http://example.com/event/");

            sess.subscribe("event:simple", Simple.class, new Autobahn.OnEventHandler() {

               @Override
               public void onEvent(String topicUri, Object event) {
                  Simple simple = (Simple) event;
                  alert("Event received : " + simple.toString());
               }
            });
         }

         @Override
         public void onClose(int code, String reason) {
            mStatusline.setText("Connection closed.");
            alert(reason);
            mStart.setEnabled(true);
         }
      });
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {

      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);


      mHostname = (EditText) findViewById(R.id.hostname);
      mPort = (EditText) findViewById(R.id.port);
      mStatusline = (TextView) findViewById(R.id.statusline);

      mStart = (Button) findViewById(R.id.start);
      mStart.setOnClickListener(new Button.OnClickListener() {

         public void onClick(View v) {
            mStart.setEnabled(false);
            test();
         }

      });
   }
}