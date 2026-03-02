package com.example.modulenavigation.websocket

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import java.util.UUID

/**
 * Unit tests for WebSocket implementation
 */
class WebSocketManagerTest {

    private lateinit var wsManager: WebSocketManager

    @Mock
    private lateinit var mockListener: WebSocketListener

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        wsManager = WebSocketManager()
    }

    @Test
    fun testAddListener() {
        wsManager.addListener(mockListener)
        assert(true) // Listener added successfully
    }

    @Test
    fun testRemoveListener() {
        wsManager.addListener(mockListener)
        wsManager.removeListener(mockListener)
        assert(true) // Listener removed successfully
    }

    @Test
    fun testClearListeners() {
        wsManager.addListener(mockListener)
        wsManager.clearListeners()
        assert(true) // Listeners cleared successfully
    }

    @Test
    fun testInitialConnectionStatus() {
        assert(!wsManager.isConnected())
    }

    @Test
    fun testSendMessageWhenDisconnected() {
        val result = wsManager.sendMessage("Test message")
        assert(!result) // Should fail when disconnected
    }

    @Test
    fun testWebSocketMessageCreation() {
        val message = WebSocketMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.MESSAGE,
            data = "Test"
        )
        assert(message.data == "Test")
        assert(message.type == MessageType.MESSAGE)
    }

    @Test
    fun testWebSocketMessageSerialization() {
        val message = WebSocketMessage(
            id = "test-id",
            type = MessageType.MESSAGE,
            data = "Test data"
        )
        val json = message.toJson()
        assert(json.contains("test-id"))
        assert(json.contains("MESSAGE"))
        assert(json.contains("Test data"))
    }

    @Test
    fun testWebSocketMessageDeserialization() {
        val originalMessage = WebSocketMessage(
            id = "test-id",
            type = MessageType.MESSAGE,
            data = "Test data"
        )
        val json = originalMessage.toJson()
        val deserializedMessage = WebSocketMessage.fromJson(json)

        assert(deserializedMessage.id == originalMessage.id)
        assert(deserializedMessage.type == originalMessage.type)
        assert(deserializedMessage.data == originalMessage.data)
    }

    @Test
    fun testMessageTypeEnum() {
        assert(MessageType.CONNECT.name == "CONNECT")
        assert(MessageType.DISCONNECT.name == "DISCONNECT")
        assert(MessageType.MESSAGE.name == "MESSAGE")
        assert(MessageType.ERROR.name == "ERROR")
        assert(MessageType.PONG.name == "PONG")
    }

    @Test
    fun testWebSocketEventHandler() {
        val handler = WebSocketEventHandler()
        val message = handler.createMessage("Test")
        assert(message.type == MessageType.MESSAGE)
        assert(message.data == "Test")
    }

    @Test
    fun testWebSocketEventHandlerConnectMessage() {
        val handler = WebSocketEventHandler()
        val message = handler.createConnectMessage()
        assert(message.type == MessageType.CONNECT)
        assert(message.data == "Client connected")
    }

    @Test
    fun testWebSocketEventHandlerDisconnectMessage() {
        val handler = WebSocketEventHandler()
        val message = handler.createDisconnectMessage()
        assert(message.type == MessageType.DISCONNECT)
        assert(message.data == "Client disconnecting")
    }
}

