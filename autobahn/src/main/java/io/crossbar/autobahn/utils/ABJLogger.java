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
        if (DEBUG) LOGGER.logp(Level.FINER, LOGGER.getName(), null, msg);
    }

    @Override
    public void v(String msg, Throwable throwable) {
        if (DEBUG) LOGGER.logp(Level.FINER, LOGGER.getName(), null, msg, throwable);
    }

    @Override
    public void d(String msg) {
        if (DEBUG) LOGGER.logp(Level.FINE, LOGGER.getName(), null, msg);
    }

    @Override
    public void i(String msg) {
        LOGGER.logp(Level.INFO, LOGGER.getName(), null, msg);
    }

    @Override
    public void w(String msg) {
        LOGGER.logp(Level.WARNING, LOGGER.getName(), null, msg);
    }

    @Override
    public void w(String msg, Throwable throwable) {
        LOGGER.logp(Level.WARNING, LOGGER.getName(), null, msg, throwable);
    }

    @Override
    public void e(String msg) {
        LOGGER.logp(Level.SEVERE, LOGGER.getName(), null, msg);
    }
}
