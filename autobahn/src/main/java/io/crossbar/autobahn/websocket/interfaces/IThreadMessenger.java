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

package io.crossbar.autobahn.websocket.interfaces;

public interface IThreadMessenger {
    void notify(Object message);
    void postDelayed(Runnable runnable, long delayMillis);
    void setOnMessageListener(OnMessageListener listener);
    void cleanup();
    interface OnMessageListener {
        void onMessage(Object message);
    }
}
