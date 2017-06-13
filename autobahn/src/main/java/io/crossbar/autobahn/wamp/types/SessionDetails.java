package io.crossbar.autobahn.wamp.types;

public class SessionDetails {
    public String realm;
    public long sessionID;

    public SessionDetails(String realm, long sessionID) {
        this.realm = realm;
        this.sessionID = sessionID;
    }
}
