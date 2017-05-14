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
     * @param buffer ByteBuffer to wrap as input stream.
     */
    public ByteBufferInputStream(ByteBuffer buffer) {
        mBuffer = buffer;
    }

    /**
     * Read one byte from input stream and advance.
     *
     * @return Byte read or -1 when stream end reached.
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
     * @param bytes Read bytes into byte array.
     * @param off   Read bytes into byte array beginning at this offset.
     * @param len   Read at most this many bytes.
     * @return Actual number of bytes read.
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
