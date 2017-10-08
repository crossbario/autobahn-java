package io.crossbar.autobahn.utils;

import android.util.Log;

import static io.crossbar.autobahn.utils.Globals.DEBUG;

class ABALogger implements IABLogger {

    private final String mTag;

    public ABALogger(String tag) {
        mTag = tag;
    }

    @Override
    public void v(String msg) {
        if (DEBUG) Log.v(mTag, msg);
    }

    @Override
    public void v(String msg, Throwable throwable) {
        if (DEBUG) Log.v(mTag, msg, throwable);
    }

    @Override
    public void d(String msg) {
        if (DEBUG) Log.d(mTag, msg);
    }

    @Override
    public void i(String msg) {
        Log.i(mTag, msg);
    }

    @Override
    public void w(String msg) {
        Log.w(mTag, msg);
    }

    @Override
    public void w(String msg, Throwable throwable) {
        Log.w(mTag, msg, throwable);
    }

    @Override
    public void e(String msg) {
        Log.e(mTag, msg);
    }
}
