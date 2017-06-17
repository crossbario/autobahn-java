package io.crossbar.autobahn.wamp.utils;

public class Cast {
    public static long castRequestID(Object object) {
        try {
            return (int) object;
        } catch (ClassCastException ignore) {
            return (long) object;
        }
    }
}
