package io.crossbar.autobahn.wamp.utils;

import io.crossbar.autobahn.wamp.interfaces.ITransport;

public class Platform {

    /**
     * Checks if code is running on Android.
     *
     * @return boolean representing whether the underlying platform
     *     is Android based
     */
    public static boolean isAndroid() {
        return System.getProperty("java.vendor").equals("The Android Project");
    }

    /**
     * Automatically returns a WebSocket based transport for WAMP based on the
     * underlying platform.
     *
     * @param webSocketURL websocket url to use for initializing of the transport
     * @return an instance of ITransport suitable for the underlying platform.
     * @throws RuntimeException most probably if the path of transport we are trying
     *     to initialize changed OR its constructor changed.
     */
    public static ITransport autoSelectTransport(String webSocketURL) throws RuntimeException {
        Class<?> transportClass;

        try {
            if (isAndroid()) {
                transportClass = Class.forName("io.crossbar.autobahn.wamp.transports.AndroidWebSocket");
            } else {
                transportClass = Class.forName("io.crossbar.autobahn.wamp.transports.NettyTransport");
            }

            return (ITransport) transportClass.getConstructor(String.class).newInstance(webSocketURL);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
