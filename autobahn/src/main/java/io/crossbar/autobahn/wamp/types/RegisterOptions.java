package io.crossbar.autobahn.wamp.types;

import java.util.Map;

public class RegisterOptions {
    private String mMatch;
    private String mInvoke;
    private int mConcurrency;
    private String mDetailsArg;

    public RegisterOptions(String match, String invoke, int concurrency, String detailsArg) {
        mMatch = match;
        mInvoke = invoke;
        mConcurrency = concurrency;
        mDetailsArg = detailsArg;
    }

    public Map<String, Object> message_attr() {
        // TODO: implement.
        return null;
    }
}
