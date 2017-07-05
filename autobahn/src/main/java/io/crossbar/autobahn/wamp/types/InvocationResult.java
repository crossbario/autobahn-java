///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InvocationResult {
    public final List<Object> results;
    public final Map<String, Object> kwresults;

    /// Default constructor.
    public InvocationResult() {
        results = null;
        kwresults = null;
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
        this.kwresults = null;
    }

    /// Constructor for positional-only results.
    public InvocationResult(List<Object> results) {
        this.results = results;
        this.kwresults = null;
    }

    /// Constructor for keyword-based-only results.
    public InvocationResult(Map<String, Object> kwresults) {
        this.kwresults = kwresults;
        this.results = null;
    }

    /// Constructor for results that have both positional and keyword-based results.
    public InvocationResult(List<Object> results, Map<String, Object> kwresults) {
        this.results = results;
        this.kwresults = kwresults;
    }
}
