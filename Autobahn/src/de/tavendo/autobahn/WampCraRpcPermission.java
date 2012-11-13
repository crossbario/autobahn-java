package de.tavendo.autobahn;

public class WampCraRpcPermission{
    
    boolean call;
    String uri;
    
    public WampCraRpcPermission() {
    }
    
    public WampCraRpcPermission(boolean call, String uri) {
        this.call = call;
        this.uri = uri;
    }
    public boolean isCall() {
        return call;
    }
    public void setCall(boolean call) {
        this.call = call;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    
    
}
