WebSocket Echo Client
=====================

This example implements a simple WebSocket client that send message to
a WebSocket server which is supposed to echo back the message. The message
is then shown in a popup.

For information on how to install AutobahnAndroid and get up and running, please
check out the [Get Started](http://autobahn.ws/android/getstarted).

WebSocket Echo Server
---------------------

When you don't have a WebSocket echo server around, you can use the
[WebSocket Echo example](http://autobahn.ws/python/tutorials/echo) provided with AutobahnPython.


	cd scm/AutobahnPython/examples/websocket/echo
	python server.py


Running
-------

 1. Build and run the app.
 2. Enter the IP and port of your WebSocket echo server.
 3. Press "connect".
 4. Enter your message and press "send".

For convenience, the app will remember the server IP/port as app settings.
