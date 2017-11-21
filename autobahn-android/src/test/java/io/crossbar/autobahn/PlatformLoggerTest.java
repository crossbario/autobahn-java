package io.crossbar.autobahn;

import org.junit.Test;

import io.crossbar.autobahn.utils.ABALogger;
import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;

import static org.junit.Assert.assertTrue;


/**
 * Tests the ABALogger for Android
 */
public class PlatformLoggerTest {
    @Test
    public void abLoggerShouldProvideABALogger() {
        // Set the java.vendor property to match what Android would say.
        // Note: This is only temporary!  This property does not persist through tests.
        System.setProperty("java.vendor", "The Android Project");

        try {
            IABLogger logger = ABLogger.getLogger("TEST");

            assertTrue("Logger was not ABALogger, was " + logger.getClass().getSimpleName(),
                    logger instanceof ABALogger);
        } catch (RuntimeException e) {
            // If the exception is coming from ABLogger.getLogger, then we'll have a cause.
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }

            assertTrue("Failed to get ABALogger", false);
        }
    }
}
