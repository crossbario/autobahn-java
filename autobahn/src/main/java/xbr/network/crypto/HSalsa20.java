/*
 * Copyright © 2017 Coda Hale (coda.hale@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xbr.network.crypto;

import java.nio.charset.StandardCharsets;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.util.Pack;

/** An implementation of the HSalsa20 hash based on the Bouncy Castle Salsa20 core. */
class HSalsa20 {

    private static final byte[] SIGMA = "expand 32-byte k".getBytes(StandardCharsets.US_ASCII);
    private static final int SIGMA_0 = Pack.littleEndianToInt(SIGMA, 0);
    private static final int SIGMA_4 = Pack.littleEndianToInt(SIGMA, 4);
    private static final int SIGMA_8 = Pack.littleEndianToInt(SIGMA, 8);
    private static final int SIGMA_12 = Pack.littleEndianToInt(SIGMA, 12);

    static void hsalsa20(byte[] out, byte[] in, byte[] k) {
        final int[] x = new int[16];

        final int in0 = Pack.littleEndianToInt(in, 0);
        final int in4 = Pack.littleEndianToInt(in, 4);
        final int in8 = Pack.littleEndianToInt(in, 8);
        final int in12 = Pack.littleEndianToInt(in, 12);

        x[0] = SIGMA_0;
        x[1] = Pack.littleEndianToInt(k, 0);
        x[2] = Pack.littleEndianToInt(k, 4);
        x[3] = Pack.littleEndianToInt(k, 8);
        x[4] = Pack.littleEndianToInt(k, 12);
        x[5] = SIGMA_4;
        x[6] = in0;
        x[7] = in4;
        x[8] = in8;
        x[9] = in12;
        x[10] = SIGMA_8;
        x[11] = Pack.littleEndianToInt(k, 16);
        x[12] = Pack.littleEndianToInt(k, 20);
        x[13] = Pack.littleEndianToInt(k, 24);
        x[14] = Pack.littleEndianToInt(k, 28);
        x[15] = SIGMA_12;

        Salsa20Engine.salsaCore(20, x, x);

        x[0] -= SIGMA_0;
        x[5] -= SIGMA_4;
        x[10] -= SIGMA_8;
        x[15] -= SIGMA_12;
        x[6] -= in0;
        x[7] -= in4;
        x[8] -= in8;
        x[9] -= in12;

        Pack.intToLittleEndian(x[0], out, 0);
        Pack.intToLittleEndian(x[5], out, 4);
        Pack.intToLittleEndian(x[10], out, 8);
        Pack.intToLittleEndian(x[15], out, 12);
        Pack.intToLittleEndian(x[6], out, 16);
        Pack.intToLittleEndian(x[7], out, 20);
        Pack.intToLittleEndian(x[8], out, 24);
        Pack.intToLittleEndian(x[9], out, 28);
    }
}
