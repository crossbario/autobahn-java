WAMP PubSub Client
==================

This example implements a simple WAMP PubSub client. The client subscribes
to a specific topic and upon receiving events on the subscribed topic,
presents the received information.

For information on how to install AutobahnAndroid and get up and running, please
check out the [Get Started](http://autobahn.ws/android/getstarted).


WAMP PubSub Server
------------------

Obviously, you will need a WAMP server that provides the PubSub topics
and message brokering used by the client.

To make this easy, we have included a special mode with [AutobahnTestsuite](http://autobahn.ws/testsuite) that
provides an embedded WAMP server.

Setting up AutobahnTestsuite is easy. Here is a complete [usage guide](http://autobahn.ws/testsuite/usage).

For the impatient, here is the quick install:

1. Install Python
2. Install setuptools
3. easy_install autobahntestsuite

You can then start the embedded WAMP server by doing

	wstest -d -m wampserver -w ws://localhost:9000


Open a browser on

	http://localhost:9000

You can open one or more browser windows/tabs to check out PubSub and also send events
to the Android PubSub demo client (by pressing the middle button "Publish Event").

If you want to start hacking on your own WAMP server, have a look at [AutobahnPython](http://autobahn.ws/python) and the [WAMP PubSub tutorial](http://autobahn.ws/python/tutorials/pubsub) there
or check out different [WAMP server implementations](http://wamp.ws/implementations).

Running
-------

 1. Build and run the app.
 2. Enter the IP and port of your WAMP server.
 3. Press "connect".

The topic will be subscribed automatically and received event information is shown in popups.

For convenience, the app will remember the server IP/port as app settings.
