package io.crossbar.autobahn.demogallery.netty;

import java.util.ArrayList;
import java.util.List;

import io.crossbar.autobahn.wamp.NettyTransport;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;

public class EchoClient implements ITransportHandler {
    private Session mSession;
    private String mRealm;

    public EchoClient(String uri, String realm) {
        mRealm = realm;
        List<String> protocols = new ArrayList<>();
        protocols.add("wamp.2.cbor");
        NettyTransport transport = new NettyTransport();
        transport.connect(uri, protocols, this);
        mSession = new Session();
        mSession.attach(transport);
        mSession.addOnJoinListener(System.out::println);
    }

    public void start() {
        mSession.join(mRealm, null);
    }

    @Override
    public void onConnect(ITransport transport) {

    }

    @Override
    public void onMessage(IMessage message) {

    }

    @Override
    public void onDisconnect(boolean wasClean) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
