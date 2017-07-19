///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.types;

public class WebSocketOptions {

    private int mMaxFramePayloadSize;

    public WebSocketOptions() {
        mMaxFramePayloadSize = 128 * 1024;
    }

    /**
     * Set maximum frame payload size that will be accepted
     * when receiving.
     * <p>
     * DEFAULT: 4MB
     *
     * @param size Maximum size in octets for frame payload.
     */
    public void setMaxFramePayloadSize(int size) {
        if (size > 0) {
            mMaxFramePayloadSize = size;
        }
    }

    /**
     * Get maximum frame payload size that will be accepted
     * when receiving.
     *
     * @return Maximum size in octets for frame payload.
     */
    public int getMaxFramePayloadSize() {
        return mMaxFramePayloadSize;
    }
}
