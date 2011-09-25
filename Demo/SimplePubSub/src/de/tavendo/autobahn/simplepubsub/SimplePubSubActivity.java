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
import android.util.Log;
import de.tavendo.autobahn.Autobahn;
import de.tavendo.autobahn.AutobahnConnection;

public class SimplePubSubActivity extends Activity {

   static final String TAG = "de.tavendo.autobahn.simplerpc";

   static private class Simple {
      public int num;
      public String name;
      public String value;

      @Override
      public String toString() {
         return "{name: " + name + ", value: " + value + ", num: " + num + "}";
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      final AutobahnConnection sess = new AutobahnConnection();

      sess.connect("ws://192.168.2.35:9000", new Autobahn.OnSession() {

         @Override
         public void onOpen() {
            Log.d(TAG, "Autobahn session opened");

            sess.prefix("event", "http://example.com/event/");

            sess.subscribe("event:simple", Simple.class, new Autobahn.OnEventHandler() {

               @Override
               public void onEvent(String topicUri, Object event) {
                  Simple simple = (Simple) event;
                  Log.d(TAG, "Received event : " + simple.toString());
               }
            });
         }

         @Override
         public void onClose(int code, String reason) {
            Log.d(TAG, "Autobahn session closed (" + code + " - " + reason + ")");
         }
      });

   }
}