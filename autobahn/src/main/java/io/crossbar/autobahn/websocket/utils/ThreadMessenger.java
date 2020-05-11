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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;

public class ThreadMessenger implements IThreadMessenger {

    private OnMessageListener mListener;
    private ScheduledExecutorService mExecutor;

    public ThreadMessenger() {
        mExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void notify(Object message) {
        if (mListener != null) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onMessage(message);
                }
            });
        }
    }

    @Override
    public void postDelayed(Runnable runnable, long delayMillis) {
        mExecutor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setOnMessageListener(OnMessageListener listener) {
        mListener = listener;
    }

    @Override
    public void cleanup() {
        mExecutor.shutdown();
    }
}
