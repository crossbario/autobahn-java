package io.crossbar.autobahn.wamp.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;

public class Hello extends Message {

    private String mRealm;
    private Map<String, Map> mRoles;

    public static final int MESSAGE_TYPE = 1;

    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(mRealm);
        Map<String, Object> details = new HashMap<>();
        details.put("roles", mRoles);
        marshaled.add(details);
        return marshaled;
    }

    public static Hello parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for HELLO", wmsg.size()));
        }

        String realm = (String) wmsg.get(1);

        Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
        Map<String, Map> roles = (Map<String, Map>) details.get("roles");

        return new Hello(realm, roles);
    }

    public Hello(String realm, Map<String, Map> roles) {
        mRealm = realm;
        mRoles = roles;
    }
}
