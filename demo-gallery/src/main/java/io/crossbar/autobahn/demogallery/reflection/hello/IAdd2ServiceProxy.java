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

package io.crossbar.autobahn.demogallery.reflection.hello;

import io.crossbar.autobahn.wamp.reflectionRoles.WampProcedure;

import java.util.concurrent.CompletableFuture;

public interface IAdd2ServiceProxy {
    @WampProcedure("com.example.add2")
    CompletableFuture<Integer> add2Async(int x, int y);
}
