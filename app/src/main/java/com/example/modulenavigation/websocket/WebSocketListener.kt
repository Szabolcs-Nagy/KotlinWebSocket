package com.example.modulenavigation.websocket

/**
 * Interface for WebSocket event listeners
 */
interface WebSocketListener {
    /**
     * Called when a message is received
     */
    fun onMessageReceived(message: WebSocketMessage)

    /**
     * Called when WebSocket connection is established
     */
    fun onConnected()

    /**
     * Called when WebSocket connection is closed
     */
    fun onDisconnected(reason: String? = null)

    /**
     * Called when an error occurs
     */
    fun onError(error: Throwable)
}

