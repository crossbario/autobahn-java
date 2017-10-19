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

public class Hello implements IMessage {

    public static final int MESSAGE_TYPE = 1;

    public final String realm;
    public final Map<String, Map> roles;
    public final List<String> authMethods;
    public final String authId;
    public final String authRole;
    public final Map<String, Object> authExtra;

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(realm);
        Map<String, Object> details = new HashMap<>();
        if (roles != null && !roles.isEmpty()) {
            details.put("roles", roles);
        }
        if (authMethods != null && !authMethods.isEmpty()) {
            details.put("authmethods", authMethods);
        }
        if (authId != null && !authId.isEmpty()) {
            details.put("authid", authId);
        }
        if (authRole != null && !authRole.isEmpty()) {
            details.put("authrole", authRole);
        }
        if (authExtra != null && !authExtra.isEmpty()) {
            details.put("authextra", authExtra);
        }
        marshaled.add(details);
        return marshaled;
    }

    public static Hello parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "HELLO", 3);

        String realm = (String) wmsg.get(1);

        Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
        Map<String, Map> roles = (Map<String, Map>) details.get("roles");

        return new Hello(realm, roles, null, null, null, null);
    }

    public Hello(String realm, Map<String, Map> roles,List<String> authMethods, String authId, String authRole, Map<String, Object> authExtra) {
        this.realm = realm;
        this.roles = roles;
        this.authMethods = authMethods;
        this.authId = authId;
        this.authRole = authRole;
        this.authExtra = authExtra;
    }
}
