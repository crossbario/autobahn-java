package io.crossbar.autobahn.wamp.types;

import java.util.Map;

public class Challenge {
    public final String method;
    public final Map<String, Object> extra;

    public Challenge(String method, Map<String, Object> extra) {
        this.method = method;
        this.extra = extra;
    }
}

public class ChallengeResponse {
    public final String signature;
    public final Map<String, Object> extra;

    public ChallengeResponse(String signature, Map<String, Object> extra) {
        this.signature = signature;
        this.extra = extra;
    }
}
