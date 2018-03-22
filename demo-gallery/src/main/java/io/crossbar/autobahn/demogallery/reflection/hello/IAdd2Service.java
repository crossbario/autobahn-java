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

public interface IAdd2Service {
    @WampProcedure("com.example.add2")
    int add2(int x, int y);
}
