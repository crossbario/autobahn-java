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

/**
 * WebSockets event handler. Users will usually provide an instance of a class
 * derived from this to handle WebSockets received messages and open/close events
 */
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

   /**
    * Fired when the WebSockets connection has been established.
    * After this happened, messages may be sent.
    */
   public void onOpen() {
   }

   /**
    * Fired when the WebSockets connection has deceased (or could
    * not established in the first place).
    *
    * @param code       Close code.
    * @param reason     Close reason (human-readable).
    */
   public void onClose(int code, String reason) {
   }

   /**
    * Fired when a text message has been received (and text
    * messages are not set to be received raw).
    *
    * @param payload    Text message payload or null (empty payload).
    */
   public void onTextMessage(String payload) {
   }

   /**
    * Fired when a text message has been received (and text
    * messages are set to be received raw).
    *
    * @param payload    Text message payload as raw UTF-8 or null (empty payload).
    */
   public void onRawTextMessage(byte[] payload) {
   }

   /**
    * Fired when a binary message has been received.
    *
    * @param payload    Binar message payload or null (empty payload).
    */
   public void onBinaryMessage(byte[] payload) {
   }

}
