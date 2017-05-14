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

package io.crossbar.autobahn;

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
     * @param size Initial size of underlying byte array.
     */
    public NoCopyByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Wraps the underyling byte array into an InputStream.
     *
     * @return New InputStream wrapping byte buffer underlying this stream.
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(buf, 0, count);
    }

    /**
     * Get byte array underlying this OutputStream. This
     * does not copy any data, but return reference to the
     * underlying byte array.
     *
     * @return Underlying byte array by reference.
     */
    public byte[] getByteArray() {
        return buf;
    }
}
