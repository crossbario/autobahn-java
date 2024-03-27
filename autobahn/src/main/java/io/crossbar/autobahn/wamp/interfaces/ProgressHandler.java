package io.crossbar.autobahn.wamp.interfaces;

import io.crossbar.autobahn.wamp.types.CallResult;

public interface ProgressHandler {
    void onProgress(CallResult result);
}
