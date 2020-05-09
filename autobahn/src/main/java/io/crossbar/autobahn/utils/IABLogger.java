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
