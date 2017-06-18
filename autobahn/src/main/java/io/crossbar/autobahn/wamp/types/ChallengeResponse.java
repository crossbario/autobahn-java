package io.crossbar.autobahn.wamp.types;

import java.util.Map;

public class ChallengeResponse {
    public final String signature;
    public final Map<String, Object> extra;

    public ChallengeResponse(String signature, Map<String, Object> extra) {
        this.signature = signature;
        this.extra = extra;
    }
}
