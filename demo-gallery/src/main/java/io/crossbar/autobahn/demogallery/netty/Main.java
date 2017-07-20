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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

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

        LOGGER.info("Service.start()");

        int returnCode = service.start(url, realm);

        LOGGER.info(String.format(".. ended with return code %s", returnCode));

        System.exit(returnCode);
    }
}
