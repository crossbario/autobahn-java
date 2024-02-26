package xbr.network.crypto;

/**
 */
public class Salsa {
    public static byte[] SIGMA = {'e', 'x', 'p', 'a', 'n', 'd', ' ', '3', '2', '-', 'b', 'y', 't', 'e', ' ', 'k'};

    // HSalsa20 applies the HSalsa20 core function to a 16-byte input in, 32-byte
    // key k, and 16-byte constant c, and returns the result as the 32-byte array
    // out.
    public static byte[] HSalsa20(byte[] in, byte[] k, byte[] c) {
        long x0 = (0xFFl & c[0]) | (0xFFl & c[1]) << 8 | (0xFFl & c[2]) << 16 | (0xFFl & c[3]) << 24;
        long x1 = (0xFFl & k[0]) | (0xFFl & k[1]) << 8 | (0xFFl & k[2]) << 16 | (0xFFl & k[3]) << 24;
        long x2 = (0xFFl & k[4]) | (0xFFl & k[5]) << 8 | (0xFFl & k[6]) << 16 | (0xFFl & k[7]) << 24;
        long x3 = (0xFFl & k[8]) | (0xFFl & k[9]) << 8 | (0xFFl & k[10]) << 16 | (0xFFl & k[11]) << 24;
        long x4 = (0xFFl & k[12]) | (0xFFl & k[13]) << 8 | (0xFFl & k[14]) << 16 | (0xFFl & k[15]) << 24;
        long x5 = (0xFFl & c[4]) | (0xFFl & c[5]) << 8 | (0xFFl & c[6]) << 16 | (0xFFl & c[7]) << 24;
        long x6 = (0xFFl & in[0]) | (0xFFl & in[1]) << 8 | (0xFFl & in[2]) << 16 | (0xFFl & in[3]) << 24;
        long x7 = (0xFFl & in[4]) | (0xFFl & in[5]) << 8 | (0xFFl & in[6]) << 16 | (0xFFl & in[7]) << 24;
        long x8 = (0xFFl & in[8]) | (0xFFl & in[9]) << 8 | (0xFFl & in[10]) << 16 | (0xFFl & in[11]) << 24;
        long x9 = (0xFFl & in[12]) | (0xFFl & in[13]) << 8 | (0xFFl & in[14]) << 16 | (0xFFl & in[15]) << 24;
        long x10 = (0xFFl & c[8]) | (0xFFl & c[9]) << 8 | (0xFFl & c[10]) << 16 | (0xFFl & c[11]) << 24;
        long x11 = (0xFFl & k[16]) | (0xFFl & k[17]) << 8 | (0xFFl & k[18]) << 16 | (0xFFl & k[19]) << 24;
        long x12 = (0xFFl & k[20]) | (0xFFl & k[21]) << 8 | (0xFFl & k[22]) << 16 | (0xFFl & k[23]) << 24;
        long x13 = (0xFFl & k[24]) | (0xFFl & k[25]) << 8 | (0xFFl & k[26]) << 16 | (0xFFl & k[27]) << 24;
        long x14 = (0xFFl & k[28]) | (0xFFl & k[29]) << 8 | (0xFFl & k[30]) << 16 | (0xFFl & k[31]) << 24;
        long x15 = (0xFFl & c[12]) | (0xFFl & c[13]) << 8 | (0xFFl & c[14]) << 16 | (0xFFl & c[15]) << 24;

        long mask = 0xFFFFFFFFl;
        for (int i = 0; i < 20; i += 2) {
            long u = mask & (x0 + x12);
            x4 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x4 + x0);
            x8 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x8 + x4);
            x12 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x12 + x8);
            x0 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x5 + x1);
            x9 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x9 + x5);
            x13 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x13 + x9);
            x1 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x1 + x13);
            x5 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x10 + x6);
            x14 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x14 + x10);
            x2 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x2 + x14);
            x6 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x6 + x2);
            x10 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x15 + x11);
            x3 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x3 + x15);
            x7 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x7 + x3);
            x11 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x11 + x7);
            x15 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x0 + x3);
            x1 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x1 + x0);
            x2 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x2 + x1);
            x3 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x3 + x2);
            x0 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x5 + x4);
            x6 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x6 + x5);
            x7 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x7 + x6);
            x4 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x4 + x7);
            x5 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x10 + x9);
            x11 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x11 + x10);
            x8 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x8 + x11);
            x9 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x9 + x8);
            x10 ^= mask & (u << 18 | u >>> (32 - 18));

            u = mask & (x15 + x14);
            x12 ^= mask & (u << 7 | u >>> (32 - 7));
            u = mask & (x12 + x15);
            x13 ^= mask & (u << 9 | u >>> (32 - 9));
            u = mask & (x13 + x12);
            x14 ^= mask & (u << 13 | u >>> (32 - 13));
            u = mask & (x14 + x13);
            x15 ^= mask & (u << 18 | u >>> (32 - 18));
        }

        byte out[] = new byte[32];
        out[0] = (byte) x0;
        out[1] = (byte) (x0 >> 8);
        out[2] = (byte) (x0 >> 16);
        out[3] = (byte) (x0 >> 24);

        out[4] = (byte) (x5);
        out[5] = (byte) (x5 >> 8);
        out[6] = (byte) (x5 >> 16);
        out[7] = (byte) (x5 >> 24);

        out[8] = (byte) (x10);
        out[9] = (byte) (x10 >> 8);
        out[10] = (byte) (x10 >> 16);
        out[11] = (byte) (x10 >> 24);

        out[12] = (byte) (x15);
        out[13] = (byte) (x15 >> 8);
        out[14] = (byte) (x15 >> 16);
        out[15] = (byte) (x15 >> 24);

        out[16] = (byte) (x6);
        out[17] = (byte) (x6 >> 8);
        out[18] = (byte) (x6 >> 16);
        out[19] = (byte) (x6 >> 24);

        out[20] = (byte) (x7);
        out[21] = (byte) (x7 >> 8);
        out[22] = (byte) (x7 >> 16);
        out[23] = (byte) (x7 >> 24);

        out[24] = (byte) (x8);
        out[25] = (byte) (x8 >> 8);
        out[26] = (byte) (x8 >> 16);
        out[27] = (byte) (x8 >> 24);

        out[28] = (byte) (x9);
        out[29] = (byte) (x9 >> 8);
        out[30] = (byte) (x9 >> 16);
        out[31] = (byte) (x9 >> 24);
        return out;
    }
}
