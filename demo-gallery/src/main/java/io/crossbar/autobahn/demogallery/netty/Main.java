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

package io.crossbar.autobahn.demogallery.netty;

import java.lang.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class Main {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        String url;
        if (args.length < 1) {
            url = "ws://localhost:8080/ws";
        } else {
            url = args[0];
        }

        String realm;
        if (args.length < 2) {
            realm = "realm1";
        } else {
            realm = args[1];
        }

        Service service = new Service(executor);

        System.out.println("service.start() ...");
        System.out.println("----------------------");

        int returnCode = service.start(url, realm);

        System.out.println("----------------------");
        System.out.println(".. ended with return code " + returnCode);

        System.exit(returnCode);
    }
}
