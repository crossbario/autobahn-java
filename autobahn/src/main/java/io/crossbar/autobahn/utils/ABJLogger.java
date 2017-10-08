package io.crossbar.autobahn.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.crossbar.autobahn.utils.Globals.DEBUG;

class ABJLogger implements IABLogger {

    private final Logger LOGGER;

    public ABJLogger(String tag) {
        LOGGER = Logger.getLogger(tag);
    }

    @Override
    public void v(String msg) {
        if (DEBUG) LOGGER.log(Level.FINER, msg);
    }

    @Override
    public void v(String msg, Throwable throwable) {
        if (DEBUG) LOGGER.log(Level.FINER, msg, throwable);
    }

    @Override
    public void d(String msg) {
        if (DEBUG) LOGGER.log(Level.FINE, msg);
    }

    @Override
    public void i(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    @Override
    public void w(String msg) {
        LOGGER.log(Level.WARNING, msg);
    }

    @Override
    public void w(String msg, Throwable throwable) {
        LOGGER.log(Level.WARNING, msg, throwable);
    }

    @Override
    public void e(String msg) {
        LOGGER.log(Level.SEVERE, msg);
    }
}
