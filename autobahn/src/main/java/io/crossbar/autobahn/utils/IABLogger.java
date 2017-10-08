package io.crossbar.autobahn.utils;

public interface IABLogger {

    void v(String msg);

    void v(String msg, Throwable throwable);

    void d(String msg);

    void i(String msg);

    void w(String msg);

    void w(String msg, Throwable throwable);

    void e(String msg);

}
