package io.crossbar.autobahn.wamp.reflectionRoles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WampException extends Exception {
    private final String mErrorUri;
    private final Map<String, Object> mDetails;
    private final List<Object> mArguments;
    private final Map<String, Object> mKwArguments;

    public WampException(String errorUri, Object... arguments) {
        this.mErrorUri = errorUri;
        this.mDetails = null;
        this.mArguments = Arrays.asList(arguments);
        this.mKwArguments = null;
    }

    public WampException(String errorUri, Map<String, Object> details, List<Object> arguments, Map<String, Object> kwArguments) {
        this.mErrorUri = errorUri;
        this.mDetails = details;
        this.mArguments = arguments;
        this.mKwArguments = kwArguments;
    }

    public String getErrorUri() {
        return mErrorUri;
    }

    public Map<String, Object> getDetails() {
        return mDetails;
    }

    public List<Object> getArguments() {
        return mArguments;
    }

    public Map<String, Object> getKwArguments() {
        return mKwArguments;
    }
}