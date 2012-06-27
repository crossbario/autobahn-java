WebSocket Broadcast Client
==========================

This example implements a simple WebSocket client that receives message
broadcast by a WebSocket broadcast server. Received messages are
displayed in a scrollbox. The client also allows to send message to the
broadcast server to received by other connected clients.

For information on how to install AutobahnAndroid and get up and running, please
check out the [Get Started](http://autobahn.ws/android/getstarted).

WebSocket Broadcast Server
--------------------------

When you don't have a WebSocket broadcast server around, you can use the
[Broadcasting over WebSocket](http://autobahn.ws/python/tutorials/broadcast) example provided with AutobahnPython.


	cd scm/AutobahnPython/examples/websocket/broadcast
	python server.py


Running
-------

 1. Build and run the app.
 2. Enter the IP and port of your WebSocket broadcast server.
 3. Press "connect".
 4. Enter your message and press "send".

For convenience, the app will remember the server IP/port as app settings.
