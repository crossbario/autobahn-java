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

public interface IPOJOService {
    @WampProcedure("io.crossbar.example.get_person")
    Person getPerson();

    @WampProcedure("io.crossbar.example.get_people")
    List<Person> getPeople();
}