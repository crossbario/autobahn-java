package io.crossbar.autobahn;

import java.io.ByteArrayOutputStream;
import java.util.Random;

public class FrameProtocol {

    private static final int MAX_PAYLOAD_NORMAL = 125;
    private static final int MAX_PAYLOAD_TWO_BYTE = 0xffff; // 2^16 - 1;
    private static final int PAYLOAD_LENGTH_EIGHT_BYTE = 127 & 0xff;

    private final Random mRng = new Random();

    public byte[] ping(byte[] payload) {
        return serializeFrame(9, payload, true, true);
    }

    public byte[] pong(byte[] payload) {
        return serializeFrame(10, payload, true, true);
    }

    public byte[] close() {
        return null;
    }

    private byte[] serializeFrame(int opcode, byte[] payload, boolean fin, boolean maskFrames) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // first octet
        byte b0 = 0;
        if (fin) {
            b0 |= (byte) (1 << 7);
        }
        b0 |= (byte) opcode;
        result.write(b0);

        // second octet
        byte b1 = 0;
        if (maskFrames) {
            b1 = (byte) (1 << 7);
        }

        long len = 0;
        if (payload != null) {
            len = payload.length;
        }

        // extended payload length
        if (len <= MAX_PAYLOAD_NORMAL) {
            b1 |= (byte) len;
            result.write(b1);
        } else if (len <= MAX_PAYLOAD_TWO_BYTE) {
            b1 |= (byte) (126 & 0xff);
            result.write(b1);
            byte[] payloadLength = new byte[]{(byte) ((len >> 8) & 0xff), (byte) (len & 0xff)};
            result.write(payloadLength, 0, payloadLength.length);
        } else {
            b1 |= (byte) (127 & 0xff);
            result.write(b1);
            byte[] payloadLength = new byte[]{
                    (byte) ((len >> 56) & 0xff),
                    (byte) ((len >> 48) & 0xff),
                    (byte) ((len >> 40) & 0xff),
                    (byte) ((len >> 32) & 0xff),
                    (byte) ((len >> 24) & 0xff),
                    (byte) ((len >> 16) & 0xff),
                    (byte) ((len >> 8) & 0xff),
                    (byte) (len & 0xff)};
            result.write(payloadLength, 0, payloadLength.length);
        }

        byte[] mask = null;
        if (maskFrames) {
            // a mask is always needed, even without payload
            mask = newFrameMask();
            result.write(mask[0]);
            result.write(mask[1]);
            result.write(mask[2]);
            result.write(mask[3]);
        }

        if (len > 0) {
            if (maskFrames) {
                /// \todo optimize masking
                /// \todo masking within buffer of output stream
                for (int i = 0; i < len; ++i) {
                    payload[i] ^= mask[i % 4];
                }
            }
            result.write(payload, 0, payload.length);
        }
        return result.toByteArray();
    }

    private byte[] newFrameMask() {
        final byte[] ba = new byte[4];
        mRng.nextBytes(ba);
        return ba;
    }
}
