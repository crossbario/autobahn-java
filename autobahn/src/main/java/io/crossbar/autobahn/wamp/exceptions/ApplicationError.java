package io.crossbar.autobahn.wamp.exceptions;

public class ApplicationError extends Error {

    // # FIXME: can be extended from here, currently only notify error URI.
    public ApplicationError(String message) {
        super(message);
    }
}
