package io.crossbar.autobahn.demogallery.netty;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class Main {

    public static void main(String[] args) {

        String url;
        if (args.length < 1) {
            url = "ws://localhost:8080/ws";
        } else {
            url = args[0];
        }
        String realm = "realm1";

        ExecutorService executor = Executors.newSingleThreadExecutor();

        EchoClient client = new EchoClient(executor, url, realm);

        int returnCode = client.start();

        System.Exit(returnCode);
    }
}
