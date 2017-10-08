package io.crossbar.autobahn.utils;

public class Globals {
    // If the AUTOBAHN_BUILD_VERSION is exported in the environment
    // we are doing a release, so disable debug logs.
    public static final boolean DEBUG = !System.getenv().containsKey("AUTOBAHN_BUILD_VERSION");
}
