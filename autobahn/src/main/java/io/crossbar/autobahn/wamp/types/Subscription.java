package io.crossbar.autobahn.wamp.types;

public class Subscription {
    public final long subscription;
    public final String topic;
    public final IEventHandler<?> handler;

    public Subscription(long subscription, String topic, IEventHandler<?> handler) {
        this.subscription = subscription;
        this.topic = topic;
        this.handler = handler;
    }
}
