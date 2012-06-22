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
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * InputStream wrapping a ByteBuffer. This class can be used i.e. to wrap
 * ByteBuffers allocated direct in NIO for socket I/O. The class does not
 * allocate ByteBuffers itself, but assumes the user has already one that
 * just needs to be wrapped to use with InputStream based processing.
 */
public class ByteBufferInputStream extends InputStream {

   /// ByteBuffer backing this input stream.
   private final ByteBuffer mBuffer;

   /**
    * Create input stream over ByteBuffer.
    *
    * @param buffer     ByteBuffer to wrap as input stream.
    */
   public ByteBufferInputStream(ByteBuffer buffer) {
      mBuffer = buffer;
   }

   /**
    * Read one byte from input stream and advance.
    *
    * @return     Byte read or -1 when stream end reached.
    */
   @Override
   public synchronized int read() throws IOException {

      if (!mBuffer.hasRemaining()) {
         return -1;
      } else {
         return mBuffer.get() & 0xFF;
      }
   }

   /**
    * Read chunk of bytes from input stream and advance. Read either as many
    * bytes specified or input stream end reached.
    *
    * @param bytes      Read bytes into byte array.
    * @param off        Read bytes into byte array beginning at this offset.
    * @param len        Read at most this many bytes.
    * @return           Actual number of bytes read.
    */
   @Override
   public synchronized int read(byte[] bytes, int off, int len)
         throws IOException {

      if (bytes == null) {
         throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > bytes.length - off) {
         throw new IndexOutOfBoundsException();
      } else if (len == 0) {
         return 0;
      }

      int length = Math.min(mBuffer.remaining(), len);
      if (length == 0) {
         return -1;
      }

      mBuffer.get(bytes, off, length);
      return length;
   }

}
