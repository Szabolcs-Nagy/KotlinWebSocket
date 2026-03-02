package com.example.modulenavigation.websocket

import org.json.JSONObject

/**
 * Data class representing a WebSocket message
 */
data class WebSocketMessage(
    val id: String,
    val type: MessageType,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(jsonString: String): WebSocketMessage {
            val json = JSONObject(jsonString)
            return WebSocketMessage(
                id = json.getString("id"),
                type = MessageType.valueOf(json.getString("type")),
                data = json.getString("data"),
                timestamp = json.getLong("timestamp")
            )
        }
    }

    fun toJson(): String {
        val json = JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("data", data)
            put("timestamp", timestamp)
        }
        return json.toString()
    }
}

enum class MessageType {
    CONNECT,
    DISCONNECT,
    MESSAGE,
    ERROR,
    PONG
}


