/******************************************************************************
 *
 *  Copyright 2011-2012 Tavendo GmbH
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
 * WebSockets connection options. This can be supplied to WebSocketConnection in connect().
 * Note that the latter copies the options provided to connect(), so any change after
 * connect will have no effect.
 */
public class WebSocketOptions {

   private int mMaxFramePayloadSize;
   private int mMaxMessagePayloadSize;
   private boolean mReceiveTextMessagesRaw;
   private boolean mTcpNoDelay;
   private int mSocketReceiveTimeout;
   private int mSocketConnectTimeout;
   private boolean mValidateIncomingUtf8;
   private boolean mMaskClientFrames;
   private int mReconnectInterval;


   /**
    * Construct default options.
    */
   public WebSocketOptions() {

      mMaxFramePayloadSize = 128 * 1024;
      mMaxMessagePayloadSize = 128 * 1024;
      mReceiveTextMessagesRaw = false;
      mTcpNoDelay = true;
      mSocketReceiveTimeout = 200;
      mSocketConnectTimeout = 6000;
      mValidateIncomingUtf8 = true;
      mMaskClientFrames = true;
      mReconnectInterval = 0;  // no reconnection by default
   }

   /**
    * Construct options as copy from other options object.
    *
    * @param other      Options to copy.
    */
   public WebSocketOptions(WebSocketOptions other) {

      mMaxFramePayloadSize = other.mMaxFramePayloadSize;
      mMaxMessagePayloadSize = other.mMaxMessagePayloadSize;
      mReceiveTextMessagesRaw = other.mReceiveTextMessagesRaw;
      mTcpNoDelay = other.mTcpNoDelay;
      mSocketReceiveTimeout = other.mSocketReceiveTimeout;
      mSocketConnectTimeout = other.mSocketConnectTimeout;
      mValidateIncomingUtf8 = other.mValidateIncomingUtf8;
      mMaskClientFrames = other.mMaskClientFrames;
      mReconnectInterval = other.mReconnectInterval;
   }

   /**
    * Receive text message as raw byte array with verified,
    * but non-decoded UTF-8.
    *
    * DEFAULT: false
    *
    * @param enabled    True to enable.
    */
   public void setReceiveTextMessagesRaw(boolean enabled) {
      mReceiveTextMessagesRaw = enabled;
   }

   /**
    * When true, WebSockets text messages are provided as
    * verified, but non-decoded UTF-8 in byte arrays.
    *
    * @return           True, iff option is enabled.
    */
   public boolean getReceiveTextMessagesRaw() {
      return mReceiveTextMessagesRaw;
   }

   /**
    * Set maximum frame payload size that will be accepted
    * when receiving.
    *
    * DEFAULT: 4MB
    *
    * @param size       Maximum size in octets for frame payload.
    */
   public void setMaxFramePayloadSize(int size) {
      if (size > 0) {
         mMaxFramePayloadSize = size;
         if (mMaxMessagePayloadSize < mMaxFramePayloadSize) {
            mMaxMessagePayloadSize = mMaxFramePayloadSize;
         }
      }
   }

   /**
    * Get maxium frame payload size that will be accepted
    * when receiving.
    *
    * @return           Maximum size in octets for frame payload.
    */
   public int getMaxFramePayloadSize() {
      return mMaxFramePayloadSize;
   }

   /**
    * Set maximum message payload size (after reassembly of fragmented
    * messages) that will be accepted when receiving.
    *
    * DEFAULT: 4MB
    *
    * @param size       Maximum size in octets for message payload.
    */
   public void setMaxMessagePayloadSize(int size) {
      if (size > 0) {
         mMaxMessagePayloadSize = size;
         if (mMaxMessagePayloadSize < mMaxFramePayloadSize) {
            mMaxFramePayloadSize = mMaxMessagePayloadSize;
         }
      }
   }

   /**
    * Get maximum message payload size (after reassembly of fragmented
    * messages) that will be accepted when receiving.
    *
    * @return           Maximum size in octets for message payload.
    */
   public int getMaxMessagePayloadSize() {
      return mMaxMessagePayloadSize;
   }

   /**
    * Set TCP No-Delay ("Nagle") for TCP connection.
    *
    * DEFAULT: true
    *
    * @param enabled    True to enable TCP No-Delay.
    */
   public void setTcpNoDelay(boolean enabled) {
      mTcpNoDelay = enabled;
   }

   /**
    * Get TCP No-Delay ("Nagle") for TCP connection.
    *
    * @return           True, iff TCP No-Delay is enabled.
    */
   public boolean getTcpNoDelay() {
      return mTcpNoDelay;
   }

   /**
    * Set receive timeout on socket. When the TCP connection disappears,
    * that will only be recognized by the reader after this timeout.
    *
    * DEFAULT: 200
    *
    * @param timeoutMs  Socket receive timeout in ms.
    */
   public void setSocketReceiveTimeout(int timeoutMs) {
      if (timeoutMs >= 0) {
         mSocketReceiveTimeout = timeoutMs;
      }
   }

   /**
    * Get socket receive timeout.
    *
    * @return           Socket receive timeout in ms.
    */
   public int getSocketReceiveTimeout() {
      return mSocketReceiveTimeout;
   }

   /**
    * Set connect timeout on socket. When a WebSocket connection is
    * about to be established, the TCP socket connect will timeout
    * after this period.
    *
    * DEFAULT: 3000
    *
    * @param timeoutMs  Socket connect timeout in ms.
    */
   public void setSocketConnectTimeout(int timeoutMs) {
      if (timeoutMs >= 0) {
         mSocketConnectTimeout = timeoutMs;
      }
   }

   /**
    * Get socket connect timeout.
    *
    * @return           Socket receive timeout in ms.
    */
   public int getSocketConnectTimeout() {
      return mSocketConnectTimeout;
   }

   /**
    * Controls whether incoming text message payload is verified
    * to be valid UTF-8.
    *
    * DEFAULT: true
    *
    * @param enabled    True to verify incoming UTF-8.
    */
   public void setValidateIncomingUtf8(boolean enabled) {
      mValidateIncomingUtf8 = enabled;
   }

   /**
    * Get UTF-8 validation option.
    *
    * @return           True, iff incoming UTF-8 is validated.
    */
   public boolean getValidateIncomingUtf8() {
      return mValidateIncomingUtf8;
   }

   /**
    * Controls whether to mask client-to-server WebSocket frames.
    * Beware, normally, WebSockets servers will deny non-masked c2s
    * frames and fail the connection.
    *
    * DEFAULT: true
    *
    * @param enabled   Set true to mask client-to-server frames.
    */
   public void setMaskClientFrames(boolean enabled) {
      mMaskClientFrames = enabled;
   }

   /**
    * Get mask client frames option.
    *
    * @return        True, iff client-to-server frames are masked.
    */
   public boolean getMaskClientFrames() {
      return mMaskClientFrames;
   }
   
   /**
    * Set reconnect interval
    * 
    * @param reconnectInterval	Interval in ms, 0 - no reconnection
    */
   public void setReconnectInterval(int reconnectInterval) {
	   mReconnectInterval = reconnectInterval;
   }
   
   public int getReconnectInterval() {
	   return mReconnectInterval;
   }
}
