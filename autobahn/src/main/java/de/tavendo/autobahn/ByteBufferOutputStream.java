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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * OutputStream wrapping a ByteBuffer. This class internally allocates a
 * direct ByteBuffer for use i.e. with NIO for socket I/O. The ByteBuffer
 * is automatically enlarged if needed (preserving contents when enlarged).
 */
public class ByteBufferOutputStream extends OutputStream {

   /// Initial size of allocated ByteBuffer.
   private final int mInitialSize;

   /// Amount to grow when ByteBuffer needs to be enlarged.
   private final int mGrowSize;

   /// Internal ByteBuffer wrapped.
   private ByteBuffer mBuffer;

   /**
    * Create a direct allocated ByteBuffer wrapped as OutputStream.
    */
   public ByteBufferOutputStream() {
      this(2 * 65536, 65536);
   }

   /**
    * Create a direct allocated ByteBuffer wrapped as OutputStream.
    *
    * @param initialSize   Initial size of ByteBuffer.
    * @param growSize      When buffer needs to grow, enlarge by this amount.
    */
   public ByteBufferOutputStream(int initialSize, int growSize) {
      mInitialSize = initialSize;
      mGrowSize = growSize;
      mBuffer = ByteBuffer.allocateDirect(mInitialSize);
      mBuffer.clear();
   }

   /**
    * Get the underlying ByteBuffer.
    *
    * @return     ByteBuffer underlying this OutputStream.
    */
   public ByteBuffer getBuffer() {
      return mBuffer;
   }

   /**
    * Calls flip on the underyling ByteBuffer.
    */
   public Buffer flip() {
      return mBuffer.flip();
   }

   /**
    * Calls clear on the underlying ByteBuffer.
    */
   public Buffer clear() {
      return mBuffer.clear();
   }

   /**
    * Calls remaining() on underlying ByteBuffer.
    */
   public int remaining() {
      return mBuffer.remaining();
   }

   /**
    * Expand the underlying ByteBuffer and preserve content.
    *
    * @param requestSize   Requested new size.
    */
   public synchronized void expand(int requestSize) {

      if (requestSize > mBuffer.capacity()) {

         ByteBuffer oldBuffer = mBuffer;
         int oldPosition = mBuffer.position();
         int newCapacity = ((requestSize / mGrowSize) + 1) * mGrowSize;
         mBuffer = ByteBuffer.allocateDirect(newCapacity);
         oldBuffer.clear();
         mBuffer.clear();
         mBuffer.put(oldBuffer);
         mBuffer.position(oldPosition);
      }
   }

   /**
    * Write one byte to the underlying ByteBuffer via this OutputStream.
    *
    * @param b  Byte to be written.
    */
   @Override
   public synchronized void write(int b) throws IOException {

      if (mBuffer.position() + 1 > mBuffer.capacity()) {
         expand(mBuffer.capacity() + 1);
      }
      mBuffer.put((byte) b);
   }

   /**
    * Write a chunk of bytes to the underyling ByteBuffer via this
    * OutputStream.
    *
    * @param bytes      Write bytes from this byte array.
    * @param off        Start reading at this offset within byte array.
    * @param len        Write this many bytes, and enlarge underyling
    *                   ByteBuffer when necessary, preserving the contents.
    */
   @Override
   public synchronized void write(byte[] bytes, int off, int len)
         throws IOException {

      if (mBuffer.position() + len > mBuffer.capacity()) {
         expand(mBuffer.capacity() + len);
      }
      mBuffer.put(bytes, off, len);
   }

   /**
    * Write a complete byte array to the underlying ByteBuffer via this
    * OutputStream.
    *
    * @param bytes   Byte array to be written.
    */
   public synchronized void write(byte[] bytes) throws IOException {
      write(bytes, 0, bytes.length);
   }

   /**
    * Write the UTF-8 encoding of a String to the underlying ByteBuffer
    * via this OutputStream.
    *
    * @param str     String to be written.
    * @throws IOException
    */
   public synchronized void write(String str) throws IOException {
      write(str.getBytes("UTF-8"));
   }

   /**
    * Write CR-LF.
    *
    * @throws IOException
    */
   public synchronized void crlf() throws IOException {
      write(0x0d);
      write(0x0a);
   }

}
