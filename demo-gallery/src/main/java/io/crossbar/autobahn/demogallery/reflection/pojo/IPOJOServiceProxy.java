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

package io.crossbar.autobahn.demogallery.reflection.pojo;

import io.crossbar.autobahn.demogallery.data.Person;
import io.crossbar.autobahn.wamp.reflectionRoles.WampProcedure;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IPOJOServiceProxy {
    @WampProcedure("io.crossbar.example.get_person")
    CompletableFuture<Person> getPersonAsync();

    @WampProcedure("io.crossbar.example.get_people")
    CompletableFuture<List<Person>> getPeopleAsync();
}
