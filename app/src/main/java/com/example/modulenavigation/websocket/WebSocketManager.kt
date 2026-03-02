package com.example.modulenavigation.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString

/**
 * WebSocket Manager handles WebSocket connections and messaging
 * Uses OkHttp for WebSocket implementation with Kotlin Coroutines for async operations
 */
class WebSocketManager(private val httpClient: OkHttpClient = OkHttpClient.Builder().build()) {

    private var webSocket: WebSocket? = null
    private val listeners = mutableListOf<WebSocketListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val messageChannel = Channel<WebSocketMessage>(Channel.BUFFERED)
    private var messageProcessingJob: Job? = null
    private var isConnecting = false
    private var isConnected = false

    /**
     * Connect to a WebSocket server
     */
    fun connect(url: String) {
        if (isConnecting || isConnected) {
            notifyError(Exception("Already connected or connecting"))
            return
        }

        isConnecting = true
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = httpClient.newWebSocket(request, WebSocketListenerImpl())
    }

    /**
     * Send a text message through WebSocket
     */
    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) != null
    }

    /**
     * Send a WebSocketMessage with metadata
     */
    fun sendWebSocketMessage(wsMessage: WebSocketMessage): Boolean {
        val json = wsMessage.toJson()
        return sendMessage(json)
    }

    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        isConnected = false
        isConnecting = false
        messageProcessingJob?.cancel()
    }

    /**
     * Add a listener for WebSocket events
     */
    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    /**
     * Clear all listeners
     */
    fun clearListeners() {
        listeners.clear()
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Cleanup resources
     */
    fun shutdown() {
        disconnect()
        scope.coroutineContext[Job]?.cancel()
    }

    private inner class WebSocketListenerImpl : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            isConnecting = false
            isConnected = true
            startMessageProcessing()
            notifyConnected()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                scope.launch {
                    val webSocketMessage = WebSocketMessage(
                        id = "test-id",
                        type = MessageType.MESSAGE,
                        data = text
                    )
//                    val message = WebSocketMessage.fromJson(text)
                    messageChannel.send(webSocketMessage)
                }
            } catch (e: Exception) {
                notifyError(e)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val text = bytes.utf8()
            onMessage(webSocket, text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            isConnected = false
            isConnecting = false
            notifyDisconnected(reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            isConnected = false
            isConnecting = false
            notifyError(t)
        }
    }

    private fun startMessageProcessing() {
        messageProcessingJob = scope.launch {
            for (message in messageChannel) {
                notifyMessageReceived(message)
            }
        }
    }

    private fun notifyConnected() {
        listeners.forEach { listener ->
            try {
                listener.onConnected()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyDisconnected(reason: String? = null) {
        listeners.forEach { listener ->
            try {
                listener.onDisconnected(reason)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyMessageReceived(message: WebSocketMessage) {
        listeners.forEach { listener ->
            try {
                listener.onMessageReceived(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyError(error: Throwable) {
        listeners.forEach { listener ->
            try {
                listener.onError(error)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

