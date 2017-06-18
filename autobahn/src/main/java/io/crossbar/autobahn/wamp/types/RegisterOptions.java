package io.crossbar.autobahn.wamp.types;

import java.util.Map;

public class RegisterOptions {
    public String match;
    public String invoke;

    public RegisterOptions(String match, String invoke) {
        this.match = match;
        this.invoke = invoke;
    }

    public Map<String, Object> message_attr() {
        // TODO: implement.
        return null;
    }
}
