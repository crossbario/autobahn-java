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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.exceptions.ProtocolError;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.utils.MessageUtil;

import static io.crossbar.autobahn.wamp.utils.Shortcuts.getOrDefault;

public class Publish implements IMessage {

    public static final int MESSAGE_TYPE = 16;

    public final long request;
    public final String topic;
    public final List<Object> args;
    public final Map<String, Object> kwargs;
    public final PublishOptions publishOptions;

    public Publish(long request, String topic, List<Object> args, Map<String, Object> kwargs,
                   PublishOptions publishOptions) {

        this.request = request;
        this.topic = topic;
        this.args = args;
        this.kwargs = kwargs;
        if(publishOptions == null){
            publishOptions = new PublishOptions(true, true, false, null);
        }
        this.publishOptions = publishOptions;
    }

    public static Publish parse(List<Object> wmsg) {
        MessageUtil.validateMessage(wmsg, MESSAGE_TYPE, "PUBLISH", 4, 6);

        long request = MessageUtil.parseLong(wmsg.get(1));
        Map<String, Object> options = (Map<String, Object>) wmsg.get(2);
        String topic = (String) wmsg.get(3);

        List<Object> args = null;
        if (wmsg.size() > 4) {
            if (wmsg.get(4) instanceof byte[]) {
                throw new ProtocolError("Binary payload not supported");
            }
            args = (List<Object>) wmsg.get(4);
        }

        Map<String, Object> kwargs = null;
        if (wmsg.size() > 5) {
            kwargs = (Map<String, Object>) wmsg.get(5);
        }

        boolean acknowledge = getOrDefault(options, "acknowledge", false);
        boolean excludeMe = getOrDefault(options, "exclude_me", true);
        boolean retain = getOrDefault(options, "retain", false);
        List<String> eligibleAuthIds = getOrDefault(options, "retain", null);
        PublishOptions publishOptions = new PublishOptions(acknowledge, excludeMe, retain, eligibleAuthIds);
        return new Publish(request, topic, args, kwargs, publishOptions);
    }

    @Override
    public List<Object> marshal() {
        List<Object> marshaled = new ArrayList<>();
        marshaled.add(MESSAGE_TYPE);
        marshaled.add(request);
        Map<String, Object> options = new HashMap<>();
        if (publishOptions.acknowledge) {
            options.put("acknowledge", publishOptions.acknowledge);
        }
        if (!publishOptions.excludeMe) {
            options.put("exclude_me", publishOptions.excludeMe);
        }
        if (publishOptions.retain) {
            options.put("retain", publishOptions.retain);
        }
        if (publishOptions.eligibleAuthIds != null && publishOptions.eligibleAuthIds.size() > 0) {
            options.put("eligible_authid", publishOptions.eligibleAuthIds);
        }
        marshaled.add(options);
        marshaled.add(topic);
        if (kwargs != null) {
            if (args == null) {
                // Empty args.
                marshaled.add(Collections.emptyList());
            } else {
                marshaled.add(args);
            }
            marshaled.add(kwargs);
        } else if (args != null) {
            marshaled.add(args);
        }
        return marshaled;
    }
}
