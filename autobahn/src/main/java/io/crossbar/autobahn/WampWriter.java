/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package io.crossbar.autobahn;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * Autobahn WAMP writer, the transmitting leg of a WAMP connection.
 * This writer serializes WAMP messages forwarded from the foreground thread
 * (the master) to this object running on the writer thread. WAMP messages are
 * serialized to JSON, and then sent via WebSockets.
 */
public class WampWriter extends WebSocketWriter {

    private static final boolean DEBUG = true;
    private static final String TAG = WampWriter.class.getName();

    /**
     * This is the Jackson JSON factory we use to create JSON generators.
     */
    private final JsonFactory mJsonFactory;

    /**
     * This is where we buffer the JSON serialization of WAMP messages.
     */
    private final NoCopyByteArrayOutputStream mPayload;

    /**
     * A writer object is created in AutobahnConnection.
     *
     * @param looper  The message looper associated with the thread running this object.
     * @param master  The message handler associated with the master thread (running AutobahnConnection).
     * @param socket  The TCP socket (channel) the WebSocket connection runs over.
     * @param options WebSockets options for the underlying WebSockets connection.
     */
    public WampWriter(Looper looper, Handler master, Socket socket,
                      WebSocketOptions options) throws IOException {

        super(looper, master, socket, options);

        mJsonFactory = new MappingJsonFactory();
        mPayload = new NoCopyByteArrayOutputStream();

        if (DEBUG) Log.d(TAG, "created");
    }

    /**
     * Called from WebSocketWriter when it receives a message in it's
     * message loop it does not recognize.
     */
    protected void processAppMessage(Object msg) throws WebSocketException, IOException {

        mPayload.reset();

        // creating a JSON generator is supposed to be a light-weight operation
        JsonGenerator generator = mJsonFactory.createJsonGenerator(mPayload);

        try {

            // serialize WAMP messages to JSON: the code here needs to understand
            // any client-to-server WAMP messages forward from the foreground thread

            if (msg instanceof WampMessage.Call) {

                WampMessage.Call call = (WampMessage.Call) msg;

                generator.writeStartArray();
                generator.writeNumber(WampMessage.MESSAGE_TYPE_CALL);
                generator.writeString(call.mCallId);
                generator.writeString(call.mProcUri);
                for (Object arg : call.mArgs) {
                    generator.writeObject(arg);
                }
                generator.writeEndArray();

            } else if (msg instanceof WampMessage.Prefix) {

                WampMessage.Prefix prefix = (WampMessage.Prefix) msg;

                generator.writeStartArray();
                generator.writeNumber(WampMessage.MESSAGE_TYPE_PREFIX);
                generator.writeString(prefix.mPrefix);
                generator.writeString(prefix.mUri);
                generator.writeEndArray();

            } else if (msg instanceof WampMessage.Subscribe) {

                WampMessage.Subscribe subscribe = (WampMessage.Subscribe) msg;

                generator.writeStartArray();
                generator.writeNumber(WampMessage.MESSAGE_TYPE_SUBSCRIBE);
                generator.writeString(subscribe.mTopicUri);
                generator.writeEndArray();

            } else if (msg instanceof WampMessage.Unsubscribe) {

                WampMessage.Unsubscribe unsubscribe = (WampMessage.Unsubscribe) msg;

                generator.writeStartArray();
                generator.writeNumber(WampMessage.MESSAGE_TYPE_UNSUBSCRIBE);
                generator.writeString(unsubscribe.mTopicUri);
                generator.writeEndArray();

            } else if (msg instanceof WampMessage.Publish) {

                WampMessage.Publish publish = (WampMessage.Publish) msg;

                generator.writeStartArray();
                generator.writeNumber(WampMessage.MESSAGE_TYPE_PUBLISH);
                generator.writeString(publish.mTopicUri);
                generator.writeObject(publish.mEvent);
                generator.writeEndArray();

            } else {

                // this should not happen, but to be sure
                throw new WebSocketException("invalid message received by AutobahnWriter");
            }
        } catch (JsonGenerationException e) {

            // this may happen, and we need to wrap the error
            throw new WebSocketException("JSON serialization error (" + e.toString() + ")");

        } catch (JsonMappingException e) {

            // this may happen, and we need to wrap the error
            throw new WebSocketException("JSON serialization error (" + e.toString() + ")");
        }

        // make sure the JSON generator has spit out everything
        generator.flush();

        // Jackson's JSON generator produces UTF-8 directly, so we send
        // a text message frame using the raw sendFrame() method
        sendFrame(1, true, mPayload.getByteArray(), 0, mPayload.size());

        // cleanup generators resources
        generator.close();
    }
}
