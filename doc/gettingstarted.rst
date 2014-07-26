
Getting Started
===============

.. container:: legacynotice

   |ab| implements version 1 of WAMP.

   This is incompatible with version 2 of WAMP which is already implemented in Autobahn|Python as well as Autobahn|JS

   Migration of |ab| to WAMP v2 is coming, but we cannot guarantee a release date.

This short intro will get you started with |ab| in no time:

* Installation
* Your first Client
* Where to go now


Installation
------------

Requirements
++++++++++++

|ab| is written against the Android 2.2 platform (= API level 8). You can use it with projects built against that version or higher.

|ab| depends on Jackson, a high-performance JSON processor. AutobahnAndroid is known to work with Jackson 1.8 and 1.9.


Set Compiler Level
++++++++++++++++++

Make sure the Java compiler compliance level is set to (at least) 1.6 in your project settings:

.. image:: /_static/autobahn_eclipse0.png


Add JARs to your project
++++++++++++++++++++++++

To use |ab| , you will need 3 JARs:

* `AutobahnAndroid <https://autobahn.s3.amazonaws.com/android/autobahn-0.5.0.jar>`_
* `Jackson Core <http://repository.codehaus.org/org/codehaus/jackson/jackson-core-asl/1.9.7/jackson-core-asl-1.9.7.jar>`_
* `Jackson Mapper <http://repository.codehaus.org/org/codehaus/jackson/jackson-mapper-asl/1.9.7/jackson-mapper-asl-1.9.7.jar>`_

You can also get a `complete bundle <https://autobahn.s3.amazonaws.com/android/autobahn-0.5.0.zip>`_ of AutobahnAndroid and Jackson JARs.

. note:: |ab| is also available as `source code <https://github.com/tavendo/AutobahnAndroid>`_ on GitHub.

Put the 3 above JARs into your project's libs folder

.. image:: /_static/autobahn_eclipse1.png

and add the JARs to the project's build path

.. image:: /_static/autobahn_eclipse2.png


Set App Permissions
+++++++++++++++++++

Make sure the Manifest for your application contains the android.permission.INTERNET

.. code-block:: java

   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android" ...>

       <uses-permission android:name="android.permission.INTERNET">
       </uses-permission>

       <application ...>
       </application>
   </manifest>

.. image:: /_static/autobahn_eclipse3.png


Your first Client
+++++++++++++++++

Create a new Android project and replace the main activity code with the following:

.. code-block:: java

   package de.tavendo.test1;

   import android.app.Activity;
   import android.os.Bundle;
   import android.util.Log;
   import de.tavendo.autobahn.WebSocketConnection;
   import de.tavendo.autobahn.WebSocketException;
   import de.tavendo.autobahn.WebSocketHandler;

   public class Test1Activity extends Activity {

      private static final String TAG = "de.tavendo.test1";

      private final WebSocketConnection mConnection = new WebSocketConnection();

      private void start() {

         final String wsuri = "ws://192.168.1.132:9000";

         try {
            mConnection.connect(wsuri, new WebSocketHandler() {

               @Override
               public void onOpen() {
                  Log.d(TAG, "Status: Connected to " + wsuri);
                  mConnection.sendTextMessage("Hello, world!");
               }

               @Override
               public void onTextMessage(String payload) {
                  Log.d(TAG, "Got echo: " + payload);
               }

               @Override
               public void onClose(int code, String reason) {
                  Log.d(TAG, "Connection lost.");
               }
            });
         } catch (WebSocketException e) {

            Log.d(TAG, e.toString());
         }
      }

      @Override
      public void onCreate(Bundle savedInstanceState) {

         super.onCreate(savedInstanceState);
         setContentView(R.layout.main);

         start();
     }
   }

Now start a WebSocket echo server on some host and adjust line 18 for the IP of the host.

.. note:: You can use any WebSocket server that implements WebSocket RFC6455 or at least Hybi-10+. A WebSocket server that only implements Hixie-76 will not work.

.. note:: For this demo, on the WebSocket server you will need to implement "echo", so that any message sent to it is simply echo'ed back to the client. I.e. you can use the server provided with the WebSocket Echo server example that comes with Autobahn|Python .

Build and run the app. You should see the following in the Android device logcat:

.. image:: /_static/autobahn_eclipse4.png


Where to go now
+++++++++++++++

Now that you are up and running, check out the :doc:`examples` available or the :doc:`API reference </_gen/packages>`.
