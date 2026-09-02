package com.developer_rahul.meetmind_ai.core.network.websocket.stomp

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class StompClient(
    private val client: OkHttpClient,
    private val url: String,
    private val headers: Map<String, String> = emptyMap()
) {
    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<StompFrame>(extraBufferCapacity = 64)
    val events: SharedFlow<StompFrame> = _events.asSharedFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    private val subscriptions = mutableMapOf<String, String>() // destination to id

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("StompClient", "WebSocket Opened")
                sendConnectFrame()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v("StompClient", "Received: $text")
                val frame = StompFrame.parse(text)
                handleFrame(frame)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("StompClient", "WebSocket Closing: $reason")
                _connectionState.tryEmit(ConnectionState.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("StompClient", "WebSocket Failure: ${t.message}")
                _connectionState.tryEmit(ConnectionState.Error(t.message ?: "Unknown error"))
            }
        })
    }

    private fun sendConnectFrame() {
        val connectHeaders = mutableMapOf(
            "accept-version" to "1.1,1.2",
            "heart-beat" to "10000,10000"
        )
        connectHeaders.putAll(headers)
        
        val frame = StompFrame("CONNECT", connectHeaders)
        sendFrame(frame)
    }

    fun subscribe(destination: String): String {
        val id = "sub-${System.currentTimeMillis()}"
        subscriptions[destination] = id
        val frame = StompFrame("SUBSCRIBE", mapOf("id" to id, "destination" to destination))
        sendFrame(frame)
        return id
    }

    fun unsubscribe(destination: String) {
        val id = subscriptions.remove(destination) ?: return
        val frame = StompFrame("UNSUBSCRIBE", mapOf("id" to id))
        sendFrame(frame)
    }

    fun send(destination: String, body: String) {
        val frame = StompFrame("SEND", mapOf("destination" to destination), body)
        sendFrame(frame)
    }

    private fun sendFrame(frame: StompFrame) {
        Log.v("StompClient", "Sending: $frame")
        webSocket?.send(frame.toString())
    }

    private fun handleFrame(frame: StompFrame) {
        when (frame.command) {
            "CONNECTED" -> _connectionState.tryEmit(ConnectionState.Connected)
            "MESSAGE" -> _events.tryEmit(frame)
            "ERROR" -> Log.e("StompClient", "STOMP Error: ${frame.body}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout")
        webSocket = null
        subscriptions.clear()
        _connectionState.tryEmit(ConnectionState.Disconnected)
    }

    sealed class ConnectionState {
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}
