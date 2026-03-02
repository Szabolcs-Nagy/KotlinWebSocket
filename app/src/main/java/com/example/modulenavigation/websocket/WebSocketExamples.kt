package com.example.modulenavigation.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * WebSocket Usage Examples
 * This file demonstrates various ways to use the WebSocket implementation
 */

/**
 * Example 1: Simple text-based messaging
 */
class SimpleWebSocketExample {
    private val wsManager = WebSocketManager()

    fun connectAndSendMessage() {
        // Create a simple listener
        val listener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                println("Received: ${message.data}")
            }

            override fun onConnected() {
                println("Connected!")
                // Send a simple message after connecting
                wsManager.sendMessage("Hello, Server!")
            }

            override fun onDisconnected(reason: String?) {
                println("Disconnected: $reason")
            }

            override fun onError(error: Throwable) {
                println("Error: ${error.message}")
            }
        }

        wsManager.addListener(listener)
        wsManager.connect("wss://echo.websocket.org")
    }

    fun disconnect() {
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

/**
 * Example 2: Using WebSocketEventHandler for structured messages
 */
class StructuredMessagingExample {
    private val wsManager = WebSocketManager()
    private val handler = WebSocketEventHandler()

    fun setup() {
        wsManager.addListener(handler)
        wsManager.connect("wss://echo.websocket.org")
    }

    fun sendStructuredMessage(data: String) {
        val message = handler.createMessage(data)
        wsManager.sendWebSocketMessage(message)
    }

    fun cleanup() {
        wsManager.removeListener(handler)
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

/**
 * Example 3: Multiple listeners for different purposes
 */
class MultiListenerExample {
    private val wsManager = WebSocketManager()

    fun setup() {
        // Listener for logging
        val loggingListener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                println("LOGGER: Message received at ${message.timestamp}")
            }

            override fun onConnected() {
                println("LOGGER: Connection established")
            }

            override fun onDisconnected(reason: String?) {
                println("LOGGER: Disconnection - $reason")
            }

            override fun onError(error: Throwable) {
                println("LOGGER: Error occurred - ${error.message}")
            }
        }

        // Listener for processing
        val processingListener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                when (message.type) {
                    MessageType.MESSAGE -> processMessage(message.data)
                    MessageType.ERROR -> handleError(message.data)
                    else -> {}
                }
            }

            override fun onConnected() {
                // Initialize some state
            }

            override fun onDisconnected(reason: String?) {
                // Cleanup state
            }

            override fun onError(error: Throwable) {
                // Error recovery
            }

            private fun processMessage(data: String) {
                println("PROCESSOR: Processing - $data")
            }

            private fun handleError(data: String) {
                println("PROCESSOR: Handling error - $data")
            }
        }

        wsManager.addListener(loggingListener)
        wsManager.addListener(processingListener)
        wsManager.connect("wss://echo.websocket.org")
    }

    fun cleanup() {
        wsManager.clearListeners()
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

/**
 * Example 4: Using with Coroutines for async message handling
 */
class CoroutineBasedExample {
    private val wsManager = WebSocketManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setupWithCoroutines() {
        val listener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                scope.launch {
                    handleMessageAsync(message)
                }
            }

            override fun onConnected() {
                scope.launch {
                    onConnectedAsync()
                }
            }

            override fun onDisconnected(reason: String?) {
                scope.launch {
                    onDisconnectedAsync(reason)
                }
            }

            override fun onError(error: Throwable) {
                scope.launch {
                    handleErrorAsync(error)
                }
            }

            private suspend fun handleMessageAsync(message: WebSocketMessage) {
                println("Processing message: ${message.id}")
                // Simulate async work
                Thread.sleep(100)
                println("Message processed: ${message.id}")
            }

            private suspend fun onConnectedAsync() {
                println("Connected, sending initial message")
                wsManager.sendMessage("Connected!")
            }

            private suspend fun onDisconnectedAsync(reason: String?) {
                println("Cleaning up after disconnect: $reason")
            }

            private suspend fun handleErrorAsync(error: Throwable) {
                println("Handling error: ${error.message}")
            }
        }

        wsManager.addListener(listener)
        wsManager.connect("wss://echo.websocket.org")
    }

    fun cleanup() {
        wsManager.disconnect()
        wsManager.shutdown()
        scope.cancel()
    }
}

/**
 * Example 5: Implementing a chat-like protocol
 */
class ChatProtocolExample {
    private val wsManager = WebSocketManager()

    data class ChatMessage(
        val username: String,
        val text: String,
        val type: String = "chat"
    )

    fun setup(username: String) {
        val listener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                try {
                    val chatMsg = parseMessage(message.data)
                    println("${chatMsg.username}: ${chatMsg.text}")
                } catch (e: Exception) {
                    println("Failed to parse message: ${e.message}")
                }
            }

            override fun onConnected() {
                val joinMsg = ChatMessage(username, "joined the chat")
                sendChatMessage(joinMsg)
            }

            override fun onDisconnected(reason: String?) {
                println("Chat disconnected: $reason")
            }

            override fun onError(error: Throwable) {
                println("Chat error: ${error.message}")
            }

            private fun parseMessage(data: String): ChatMessage {
                val parts = data.split("|")
                return ChatMessage(
                    username = parts[0],
                    text = parts[1],
                    type = parts.getOrNull(2) ?: "chat"
                )
            }
        }

        wsManager.addListener(listener)
        wsManager.connect("wss://echo.websocket.org")
    }

    fun sendChatMessage(chatMessage: ChatMessage) {
        val data = "${chatMessage.username}|${chatMessage.text}|${chatMessage.type}"
        wsManager.sendMessage(data)
    }

    fun cleanup() {
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

/**
 * Example 6: Error handling and reconnection pattern
 */
class ReconnectableWebSocketClient {
    private val wsManager = WebSocketManager()
    private val maxRetries = 3
    private var retryCount = 0
    private val retryDelay = 2000L // 2 seconds

    fun setupWithReconnection() {
        val listener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                retryCount = 0 // Reset retry count on successful message
                println("Message received: ${message.data}")
            }

            override fun onConnected() {
                retryCount = 0
                println("Connected successfully")
            }

            override fun onDisconnected(reason: String?) {
                println("Disconnected: $reason")
                attemptReconnect()
            }

            override fun onError(error: Throwable) {
                println("Error: ${error.message}")
                attemptReconnect()
            }
        }

        wsManager.addListener(listener)
        wsManager.connect("wss://echo.websocket.org")
    }

    private fun attemptReconnect() {
        if (retryCount < maxRetries) {
            retryCount++
            println("Attempting reconnect ($retryCount/$maxRetries) in ${retryDelay}ms")
            Thread {
                Thread.sleep(retryDelay)
                if (retryCount <= maxRetries) {
                    wsManager.connect("wss://echo.websocket.org")
                }
            }.start()
        } else {
            println("Max retries reached. Giving up.")
        }
    }

    fun cleanup() {
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

/**
 * Example 7: Connection state monitoring
 */
class ConnectionMonitorExample {
    private val wsManager = WebSocketManager()
    private var isHealthy = false

    fun setup() {
        val listener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                isHealthy = true
            }

            override fun onConnected() {
                isHealthy = true
                println("WebSocket health: HEALTHY")
            }

            override fun onDisconnected(reason: String?) {
                isHealthy = false
                println("WebSocket health: UNHEALTHY - $reason")
            }

            override fun onError(error: Throwable) {
                isHealthy = false
                println("WebSocket health: ERROR - ${error.message}")
            }
        }

        wsManager.addListener(listener)
        wsManager.connect("wss://echo.websocket.org")

        // Monitor health periodically
        startHealthMonitoring()
    }

    private fun startHealthMonitoring() {
        Thread {
            while (true) {
                Thread.sleep(5000) // Check every 5 seconds
                if (!isHealthy && wsManager.isConnected()) {
                    println("Health check: Connection is stale, attempting ping")
                    wsManager.sendMessage("PING")
                }
            }
        }.start()
    }

    fun cleanup() {
        wsManager.disconnect()
        wsManager.shutdown()
    }
}

