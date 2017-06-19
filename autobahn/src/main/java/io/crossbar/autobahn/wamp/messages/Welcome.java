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

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;

public class Welcome implements IMessage {

    public static final int MESSAGE_TYPE = 2;

    public final long session;
    public final Map<String, Map> roles;
    public final String realm;

    public Welcome(long session, Map<String, Map> roles, String realm) {
        this.session = session;
        this.roles = roles;
        this.realm = realm;
    }

    public static Welcome parse(List<Object> wmsg) {
        if (wmsg.size() == 0 || !(wmsg.get(0) instanceof Integer) || (int) wmsg.get(0) != MESSAGE_TYPE) {
            throw new IllegalArgumentException("Invalid message.");
        }

        if (wmsg.size() != 3) {
            throw new ProtocolError(String.format("invalid message length %s for WELCOME", wmsg.size()));
        }

        long session = (long) wmsg.get(1);

        Map<String, Object> details = (Map<String, Object>) wmsg.get(2);
        Map<String, Map> roles = (Map<String, Map>) details.get("roles");
        String realm = (String) details.get("realm");

        return new Welcome(session, roles, realm);
    }

    @Override
    public List<Object> marshal() {
        // We are a client library, so don't really need to send a Welcome.
        throw new UnsupportedOperationException("Welcome only to be sent by a server library.");
    }
}
