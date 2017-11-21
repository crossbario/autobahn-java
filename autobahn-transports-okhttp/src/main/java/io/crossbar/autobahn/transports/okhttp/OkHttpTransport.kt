package io.crossbar.autobahn.transports.okhttp

import io.crossbar.autobahn.utils.ABLogger
import io.crossbar.autobahn.wamp.interfaces.ISerializer
import io.crossbar.autobahn.wamp.interfaces.ITransport
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler
import io.crossbar.autobahn.wamp.serializers.JSONSerializer
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * OkHttp Transport for Autobahn
 * Provides an OkHttp-managed WebSocket
 */
class OkHttpTransport(private val uri: String) : ITransport {
    private val LOGGER = ABLogger.getLogger(OkHttpTransport::class.java.name)

    private val client: OkHttpClient = OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

    private val serializer: ISerializer = JSONSerializer()
    private var webSocket: WebSocket? = null

    override fun send(payload: ByteArray, isBinary: Boolean) {
        if (isBinary) {
            webSocket?.send(ByteString.of(payload, 0, payload.size))
        } else {
            webSocket?.send(payload.toString(Charsets.UTF_8))
        }
    }

    override fun connect(transportHandler: ITransportHandler?) {
        val request = Request.Builder()
                .url(uri)
                .header("Sec-WebSocket-Protocol", "wamp.2.json")
                .build()


        LOGGER.d("Connecting!")
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(socket: WebSocket?, response: Response?) {
                LOGGER.d("onOpen: $socket, $response")
                this@OkHttpTransport.webSocket = socket

                transportHandler?.onConnect(this@OkHttpTransport, serializer)
            }

            override fun onMessage(webSocket: WebSocket?, text: String) {
                LOGGER.e("onMessage: $webSocket, $text")

                transportHandler?.onMessage(text.toByteArray(Charsets.UTF_8), false)
            }

            override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                LOGGER.e("onMessage: $webSocket, $bytes")

                transportHandler?.onMessage(bytes?.toByteArray(), true)
            }

            override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                LOGGER.e("onFailure: $webSocket, $t, $response")

                this@OkHttpTransport.webSocket = null
            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                LOGGER.e("onClosing: $webSocket, $code, $reason")
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                LOGGER.e("onClosed: $webSocket, $code, $reason")

                // TODO: We should look at the code, and determine if was a clean disconnect or not.
                transportHandler?.onDisconnect(false)

                this@OkHttpTransport.webSocket = null
            }
        })
    }

    override fun isOpen(): Boolean = webSocket != null

    override fun close() {
        webSocket?.close(1000, null)
    }

    override fun abort() {
        webSocket?.cancel()
    }
}