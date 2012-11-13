package de.tavendo.autobahn;

public class WampCraPermissions {
    
    public WampCraRpcPermission[] rpc;
    public WampCraPubSubPermission[] pubsub;
    
    public WampCraPermissions() {
    }

    public WampCraPermissions(WampCraRpcPermission[] rpc, WampCraPubSubPermission[] pubsub) {
        this.rpc = rpc;
        this.pubsub = pubsub;
    }

    public WampCraRpcPermission[] getRpc() {
        return rpc;
    }

    public void setRpc(WampCraRpcPermission[] rpc) {
        this.rpc = rpc;
    }

    public WampCraPubSubPermission[] getPubsub() {
        return pubsub;
    }

    public void setPubsub(WampCraPubSubPermission[] pubsub) {
        this.pubsub = pubsub;
    }
    
    
    
}
