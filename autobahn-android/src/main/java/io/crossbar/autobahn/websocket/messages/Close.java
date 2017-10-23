package io.crossbar.autobahn.websocket.messages;

/// WebSockets close to send or received.
public class Close extends Message {

    public int mCode;
    public String mReason;
    // Not to be delivered on the wire, only for local use.
    public boolean mIsReply;

    Close() {
        mCode = -1;
        mReason = null;
    }

    Close(int code) {
        mCode = code;
        mReason = null;
    }

    // For local use only.
    public Close(int code, boolean isReply) {
        mCode = code;
        mIsReply = isReply;
    }

    public Close(int code, String reason) {
        mCode = code;
        mReason = reason;
    }

    public Close(int code, String reason, boolean isReply) {
        mCode = code;
        mIsReply = isReply;
        mReason = reason;
    }
}
