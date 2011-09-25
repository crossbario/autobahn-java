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

package de.tavendo.autobahn;

public class WebSocketHandler {

   /**
    * Connection was closed normally.
    */
   public static final int CLOSE_NORMAL = 1;

   /**
    * Connection could not be established in the first place.
    */
   public static final int CLOSE_CANNOT_CONNECT = 2;

   /**
    * A previously established connection was lost unexpected.
    */
   public static final int CLOSE_CONNECTION_LOST = 3;

   /**
    * The connection was closed because a protocol violation
    * occurred.
    */
   public static final int CLOSE_PROTOCOL_ERROR = 4;

   /**
    * Internal error.
    */
   public static final int CLOSE_INTERNAL_ERROR = 5;

   public void onOpen() {
   }

   public void onClose(int code, String reason) {
   }

   public void onTextMessage(String payload) {
   }

   public void onRawTextMessage(byte[] payload) {
   }

   public void onBinaryMessage(byte[] payload) {
   }

}
