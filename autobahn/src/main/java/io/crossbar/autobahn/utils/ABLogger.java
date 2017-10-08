package io.crossbar.autobahn.utils;

import android.util.Log;

public class ABLogger {

    private static final boolean DEBUG = true;

    private final String mTag;

    private ABLogger(String tag) {
        mTag = tag;
    }

    public static ABLogger getLogger(String tag) {
        return new ABLogger(tag);
    }

    public void i(String msg) {
        Log.i(mTag, msg);
    }

    public void d(String msg) {
        if (DEBUG) Log.d(mTag, msg);
    }

    public void v(String msg) {
        if (DEBUG) Log.v(mTag, msg);
    }

    public void v(String msg, Throwable throwable) {
        if (DEBUG) Log.v(mTag, msg, throwable);
    }

    public void e(String msg) {
        Log.e(mTag, msg);
    }

    public void w(String msg) {
        Log.w(mTag, msg);
    }

    public void w(String msg, Throwable throwable) {
        Log.w(mTag, msg, throwable);
    }
}
