package io.crossbar.autobahn.wamp.types;

public class PublishOptions {
    public final boolean acknowledge;
    public final boolean excludeMe;

    public PublishOptions(boolean acknowledge, boolean excludeMe) {
        this.acknowledge = acknowledge;
        this.excludeMe = excludeMe;
    }
}
