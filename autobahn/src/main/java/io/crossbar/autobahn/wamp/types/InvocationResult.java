package io.crossbar.autobahn.wamp.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InvocationResult {
    public List<Object> results;
    public Map<String, Object> kwresults;

    /// Default constructor.
    public InvocationResult() {
    }

    /// Copy constructor.
    public InvocationResult(InvocationResult other) {
        this.results = other.results;
        this.kwresults = other.kwresults;
    }

    /// Convenience constructor for single, positional returns.
    public InvocationResult(Object result) {
        this.results = new ArrayList<>();
        this.results.add(result);
    }

    /// Constructor for positional-only results.
    public InvocationResult(List<Object> results) {
        this.results = results;
    }

    /// Constructor for keyword-based-only results.
    public InvocationResult(Map<String, Object> kwresults) {
        this.kwresults = kwresults;
    }

    /// Constructor for results that have both positional and keyword-based results.
    public InvocationResult(List<Object> results, Map<String, Object> kwresults) {
        this.results = results;
        this.kwresults = kwresults;
    }
}
