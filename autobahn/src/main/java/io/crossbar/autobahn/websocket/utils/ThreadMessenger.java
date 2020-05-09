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

package io.crossbar.autobahn.websocket.utils;

import java.util.Timer;
import java.util.TimerTask;

import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;

public class ThreadMessenger implements IThreadMessenger {

    private Timer mTimer;
    private OnMessageListener mListener;

    public ThreadMessenger() {
        mTimer = new Timer();
    }

    public void notify(Object message) {
        if (mListener != null) {
            mListener.onMessage(message);
        }
    }

    @Override
    public void postDelayed(Runnable runnable, long delayMillis) {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delayMillis);
    }

    @Override
    public void setOnMessageListener(OnMessageListener listener) {
        mListener = listener;
    }
}
