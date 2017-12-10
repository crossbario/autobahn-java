///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Hello implements IMessage {

    public static final int MESSAGE_TYPE = 1;

    public final String realm;
    public final Map<String, Map> roles;
    public final List<String> authMethods;
    public final String authID;
    public final Map<String, Object> authextra;

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(realm);
        Map<String, Object> details = new HashMap<>();
        details.put("roles", roles);
        if (authMethods != null) {
            details.put("authmethods", authMethods);
        }
        if (authID != null) {
            details.put("authid", authID);
        }
        if (authextra != null) {
            details.put("authextra", authextra);
        }
        marshaled.add(details);
        return marshaled;
    }

    public static Hello parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "HELLO", 3);

        String realm = (String) wmsg.get(1);

        Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
        Map<String, Map> roles = (Map<String, Map>) details.get("roles");
        List<String> authMethods = getOrDefault(details, "authmethods", null);
        String authID = getOrDefault(details, "authid", null);
        Map<String, Object> authextra = getOrDefault(details, "authextra", null);

        return new Hello(realm, roles, authMethods, authID, authextra);
    }

    public Hello(String realm, Map<String, Map> roles) {
        this(realm, roles, null, null, null);
    }

    public Hello(String realm, Map<String, Map> roles, List<String> authMethods, String authID,
                 Map<String, Object> authextra) {
        this.realm = realm;
        this.roles = roles;
        this.authMethods = authMethods;
        this.authID = authID;
        this.authextra = authextra;
    }
}
