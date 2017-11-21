package io.crossbar.autobahn;

import org.junit.Test;

import io.crossbar.autobahn.utils.ABALogger;
import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.utils.Platform;

import static org.junit.Assert.assertTrue;

/**
 * Created by cgoldberg02 on 11/21/17.
 */

public class PlatformTransportTest {
    @Test
    public void transportResolvesToAndroidWebSocket() {
        // Set the java.vendor property to match what Android would say.
        // Note: This is only temporary!  This property does not persist through tests.
        System.setProperty("java.vendor", "The Android Project");

        try {
            ITransport transport = Platform.autoSelectTransport("hello.");

            // We would expect to see AndroidWebSocket, if on Android.
            assertTrue("Transport was not AndroidWebSocket, was " + transport.getClass().getSimpleName(),
                    transport instanceof ABALogger);
        } catch (RuntimeException e) {
            // If the exception is coming from ABLogger.getLogger, then we'll have a cause.
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }

            assertTrue("Failed to get Transport", false);
        }
    }
}
