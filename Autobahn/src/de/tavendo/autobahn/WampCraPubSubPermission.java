package de.tavendo.autobahn;

public class WampCraPubSubPermission{
    
    boolean prefix;
    String uri;
    boolean pub;
    boolean sub;
    
    public WampCraPubSubPermission() {
    }
       
    public WampCraPubSubPermission(boolean prefix, String uri, boolean pub, boolean sub) {
        this.prefix = prefix;
        this.uri = uri;
        this.pub = pub;
        this.sub = sub;
    }
    
    public boolean isPrefix() {
        return prefix;
    }
    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public boolean isPub() {
        return pub;
    }
    public void setPub(boolean pub) {
        this.pub = pub;
    }
    public boolean isSub() {
        return sub;
    }
    public void setSub(boolean sub) {
        this.sub = sub;
    }
    
    
}   
