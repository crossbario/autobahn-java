package io.crossbar.autobahn;

import io.crossbar.autobahn.utils.Globals;

public abstract class Autobahn {

    public static void enableDebugLog(boolean enable) {
        Globals.DEBUG = enable;
    }

}
