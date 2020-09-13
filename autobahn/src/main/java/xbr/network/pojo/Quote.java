package xbr.network.pojo;

import java.math.BigInteger;

import xbr.network.Util;

public class Quote {
    public byte[] price;
    public long timestamp;
    public byte[] key;
    public long expires;

    public BigInteger getPriceBigInt() {
        return Util.toXBR(price);
    }
}
