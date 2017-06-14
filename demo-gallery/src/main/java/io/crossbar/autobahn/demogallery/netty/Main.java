package io.crossbar.autobahn.demogallery.netty;

public class Main {
    public static void main(String[] args) {
        String url;
        if (args.length < 1) {
            url = "ws://localhost:8080/ws";
        } else {
            url = args[0];
        }
        new EchoClient(url, "realm1").start();
    }
}
