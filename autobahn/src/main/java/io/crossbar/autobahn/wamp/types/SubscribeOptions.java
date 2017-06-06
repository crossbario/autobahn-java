package io.crossbar.autobahn.wamp.types;

public class SubscribeOptions {
    public String match;
    public boolean details;
    public boolean get_retained;

    public SubscribeOptions(String match, boolean details, boolean get_retained) {
        this.match = match;
        this.details = details;
        this.get_retained = get_retained;
    }
}
