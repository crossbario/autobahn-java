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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * OutputStream backed by a byte array. This class provides copyless access
 * to byte array backing the ByteArrayOutputStream
 */
public class NoCopyByteArrayOutputStream extends ByteArrayOutputStream {

   /**
    * Create new OutputStream backed by byte array.
    */
   public NoCopyByteArrayOutputStream() {
      super();
   }

   /**
    * Create new OutputStream backed by byte array.
    *
    * @param size    Initial size of underlying byte array.
    */
   public NoCopyByteArrayOutputStream(int size) {
      super(size);
   }

   /**
    * Wraps the underyling byte array into an InputStream.
    *
    * @return     New InputStream wrapping byte buffer underlying this stream.
    */
   public InputStream getInputStream() {
      return new ByteArrayInputStream(buf, 0, count);
   }

   /**
    * Get byte array underlying this OutputStream. This
    * does not copy any data, but return reference to the
    * underlying byte array.
    *
    * @return     Underlying byte array by reference.
    */
   public byte[] getByteArray() {
      return buf;
   }
}
