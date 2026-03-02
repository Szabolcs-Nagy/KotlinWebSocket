package com.example.modulenavigation.websocket

import android.util.Log
import java.util.UUID

/**
 * Example implementation of WebSocketListener for handling WebSocket events
 */
class WebSocketEventHandler : WebSocketListener {

    companion object {
        private const val TAG = "WebSocketEventHandler"
    }

    override fun onMessageReceived(message: WebSocketMessage) {
        Log.d(TAG, "Message received: ${message.type} - ${message.data}")
    }

    override fun onConnected() {
        Log.d(TAG, "WebSocket connected!")
    }

    override fun onDisconnected(reason: String?) {
        Log.d(TAG, "WebSocket disconnected. Reason: $reason")
    }

    override fun onError(error: Throwable) {
        Log.e(TAG, "WebSocket error: ${error.message}", error)
    }

    /**
     * Helper function to create a text message
     */
    fun createMessage(data: String): WebSocketMessage {
        return WebSocketMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.MESSAGE,
            data = data
        )
    }

    /**
     * Helper function to create a connection message
     */
    fun createConnectMessage(): WebSocketMessage {
        return WebSocketMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.CONNECT,
            data = "Client connected"
        )
    }

    /**
     * Helper function to create a disconnection message
     */
    fun createDisconnectMessage(): WebSocketMessage {
        return WebSocketMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.DISCONNECT,
            data = "Client disconnecting"
        )
    }
}

