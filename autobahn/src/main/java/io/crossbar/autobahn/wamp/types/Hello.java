package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;

public class Hello {

    private String mRealm;
    private List<String> mAuthMethods;
    private String mAuthID;
    private String mAuthRole;
    private Map<String, Object> mAuthExtra;
    private boolean mResumable;
    private String mResumeSession;
    private String mResumeToken;

    public static final int MESSAGE_TYPE = 1;

    public static byte[] marshal(Serializer serializer) {
        return null;
    }

    public static Hello parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for HELLO", wmsg.size()));
        }

        String realm = (String) wmsg.get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) wmsg.get(2);

        return new Hello(realm, null, null, null, null, false, null, null);
    }

    public Hello(String realm, List<String> authMethods, String authID, String authRole,
                 Map<String, Object> authExtra, boolean resumable, String resumeSession,
                 String resumeToken) {
        mRealm = realm;
        mAuthMethods = authMethods;
        mAuthID = authID;
        mAuthRole = authRole;
        mAuthExtra = authExtra;
        mResumable = resumable;
        mResumeSession = resumeSession;
        mResumeToken = resumeToken;
    }
}
