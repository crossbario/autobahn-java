package io.crossbar.autobahn.wamp.types;

public class SubscribeOptions {
    public String match;
    public boolean details;
    public boolean getRetained;

    public SubscribeOptions(String match, boolean details, boolean getRetained) {
        this.match = match;
        this.details = details;
        this.getRetained = getRetained;
    }
}
