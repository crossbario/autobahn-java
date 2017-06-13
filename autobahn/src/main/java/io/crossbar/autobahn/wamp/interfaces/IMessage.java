package io.crossbar.autobahn.wamp.interfaces;

import java.util.List;

public interface IMessage {
    List<Object> marshal();
}
