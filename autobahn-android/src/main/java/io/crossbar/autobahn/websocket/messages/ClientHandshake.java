package io.crossbar.autobahn.websocket.messages;

import java.util.Map;

/// Initial WebSockets handshake (client request).
public class ClientHandshake extends Message {

    public String mHost;
    public String mPath;
    public String mQuery;
    public String mOrigin;
    public String[] mSubprotocols;
    public Map<String, String> mHeaderList;

    public ClientHandshake(String host) {
        mHost = host;
        mPath = "/";
        mOrigin = null;
        mSubprotocols = null;
        mHeaderList = null;
    }

    ClientHandshake(String host, String path, String origin) {
        mHost = host;
        mPath = path;
        mOrigin = origin;
        mSubprotocols = null;
    }

    ClientHandshake(String host, String path, String origin, String[] subprotocols) {
        mHost = host;
        mPath = path;
        mOrigin = origin;
        mSubprotocols = subprotocols;
    }
}
