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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {

    private static final String LOG_CONFIG = "handlers= java.util.logging.ConsoleHandler\n"
            + ".level = %s\n" +
            "java.util.logging.ConsoleHandler.level = %s\n" +
            "java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {

        readAndSetLogLevel();

        Executor executor = Executors.newSingleThreadExecutor();

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

//        Service service = new Service(executor);
        CIService service = new CIService(executor);

        LOGGER.info("Service.start()");

        int returnCode = service.start(url, realm);

        LOGGER.info(String.format(".. ended with return code %s", returnCode));

        System.exit(returnCode);
    }

    private static void readAndSetLogLevel() throws IOException {
        String logLevel = System.getProperty("logLevel", "FINEST");
        String config = String.format(LOG_CONFIG, logLevel, logLevel);
        InputStream stream = new ByteArrayInputStream(config.getBytes(Charset.forName("UTF-8")));
        LogManager.getLogManager().readConfiguration(stream);
    }
}
