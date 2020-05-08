package io.crossbar.autobahn.utils;

public class Platform {
    private static boolean IS_PLATFORM_CHECKED = false;
    private static boolean IS_ANDROID = false;

    /**
     * Checks if code is running on Android.
     *
     * @return boolean representing whether the underlying platform
     *     is Android based
     */
    public static boolean isAndroid() {
        if (!IS_PLATFORM_CHECKED) {
            IS_ANDROID = System.getProperty("java.vendor").equals("The Android Project");
            IS_PLATFORM_CHECKED = true;
        }
        return IS_ANDROID;
    }
}
