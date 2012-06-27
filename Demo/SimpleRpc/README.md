WAMP RPC Client
===============

This example implements a simple WAMP RPC client. The client issues a number
of RPCs on a remote WAMP server and presents the results.

For information on how to install AutobahnAndroid and get up and running, please
check out the [Get Started](http://autobahn.ws/android/getstarted).


WAMP RPC Server
---------------

Obviously, you will need a WAMP server that provides the RPC endpoints
called by the test client.

To make this easy, we have included a special mode with [AutobahnTestsuite](http://autobahn.ws/testsuite) that
provides an embedded WAMP server.

Setting up AutobahnTestsuite is easy. Here is a complete [usage guide](http://autobahn.ws/testsuite/usage).

For the impatient, here is the quick install:

1. Install Python
2. Install setuptools
3. easy_install autobahntestsuite

You can then start the embedded WAMP server by doing

	wstest -d -m wampserver -w ws://localhost:9000


If you want to create new RPC endpoint or start hacking on your own WAMP server, have
a look at [AutobahnPython](http://autobahn.ws/python) and the [WAMP RPC tutorial](http://autobahn.ws/python/tutorials/rpc) there
or check out different [WAMP server implementations](http://wamp.ws/implementations).

Running
-------

 1. Build and run the app.
 2. Enter the IP and port of your WAMP server.
 3. Press "connect".

The RPCs will be fired automatically and the results shown as popups.

For convenience, the app will remember the server IP/port as app settings.
