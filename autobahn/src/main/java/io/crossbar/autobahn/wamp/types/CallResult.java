package io.crossbar.autobahn.wamp.types;

import java.util.List;
import java.util.Map;

public class CallResult {
    public List<Object> results;
    public Map<String, Object> kwresults;

    public CallResult(List<Object> results) {
        this.results = results;
    }

    public CallResult(Map<String, Object> kwresults) {
        this.kwresults = kwresults;
    }

    public CallResult(List<Object> results, Map<String, Object> kwresults) {
        this.results = results;
        this.kwresults = kwresults;
    }
}
