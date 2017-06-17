package io.crossbar.autobahn.wamp.utils;

public class IDGenerator {
    private long mNext;

    public long next() {
        mNext += 1;
        if (mNext > 9007199254740992L) {
            mNext = 1;
        }
        return mNext;
    }
}
