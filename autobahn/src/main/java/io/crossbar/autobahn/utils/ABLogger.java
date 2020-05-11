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

package io.crossbar.autobahn.utils;

import static io.crossbar.autobahn.utils.Platform.isAndroid;

public class ABLogger {

    public static IABLogger getLogger(String tag) {
        Class<?> loggerClass;

        try {
            if (isAndroid()) {
                loggerClass = Class.forName("io.crossbar.autobahn.utils.ABALogger");
            } else {
                loggerClass = Class.forName("io.crossbar.autobahn.utils.ABJLogger");
            }

            return (IABLogger) loggerClass.getConstructor(String.class).newInstance(tag);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
