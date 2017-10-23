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

package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;
import java.util.Map;
import java8.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.ReceptionResult;


@FunctionalInterface
public interface IEventHandler {
    CompletableFuture<ReceptionResult> apply(List<Object> args,
                                             Map<String, Object> kwargs,
                                             EventDetails details);
}
