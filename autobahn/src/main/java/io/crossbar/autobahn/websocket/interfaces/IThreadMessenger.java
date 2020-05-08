package io.crossbar.autobahn.websocket.interfaces;

public interface IThreadMessenger {
    void notify(Object message);
    void postDelayed(Runnable runnable, long delayMillis);
    void setOnMessageListener(OnMessageListener listener);
    interface OnMessageListener {
        void onMessage(Object message);
    }
}
