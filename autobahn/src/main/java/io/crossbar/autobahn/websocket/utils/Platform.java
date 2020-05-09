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

import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;

public class Platform {
    public static IThreadMessenger selectThreadMessenger() throws RuntimeException {
        Class<?> messengerClass;

        try {
            if (io.crossbar.autobahn.utils.Platform.isAndroid()) {
                messengerClass = Class.forName("io.crossbar.autobahn.websocket.utils.AndroidThreadMessenger");
            } else {
                messengerClass = Class.forName("io.crossbar.autobahn.websocket.utils.ThreadMessenger");
            }

            return (IThreadMessenger) messengerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
