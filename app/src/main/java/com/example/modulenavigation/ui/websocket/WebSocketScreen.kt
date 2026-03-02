package com.example.modulenavigation.ui.websocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.modulenavigation.websocket.WebSocketEventHandler
import com.example.modulenavigation.websocket.WebSocketListener
import com.example.modulenavigation.websocket.WebSocketManager
import com.example.modulenavigation.websocket.WebSocketMessage
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WebSocketScreen() {
    val wsManager = koinInject<WebSocketManager>()
    val connectionStatus = remember { mutableStateOf("Disconnected") }
    val connectionColor = remember { mutableStateOf(Color.Red) }
    val messages = remember { mutableStateListOf<WebSocketMessage>() }
    val messageInput = remember { mutableStateOf("") }
    val wsHandler = remember { WebSocketEventHandler() }
    val serverUrl = remember { mutableStateOf("wss://echo.websocket.org") }

    LaunchedEffect(Unit) {
        val listener: WebSocketListener = object : WebSocketListener {
            override fun onMessageReceived(message: WebSocketMessage) {
                messages.add(message)
            }

            override fun onConnected() {
                connectionStatus.value = "Connected"
                connectionColor.value = Color.Green
            }

            override fun onDisconnected(reason: String?) {
                connectionStatus.value = "Disconnected"
                connectionColor.value = Color.Red
            }

            override fun onError(error: Throwable) {
                connectionStatus.value = "Error: ${error.message}"
                connectionColor.value = Color.Red
            }
        }
        wsManager.addListener(listener)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Server URL Input
        TextField(
            value = serverUrl.value,
            onValueChange = { serverUrl.value = it },
            label = { Text("WebSocket Server URL") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !wsManager.isConnected()
        )

        // Connection Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(connectionColor.value)
        ) {
            Text(
                text = "Status: ${connectionStatus.value}",
                modifier = Modifier.padding(16.dp),
                color = Color.White
            )
        }

        // Connect/Disconnect Buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { wsManager.connect(serverUrl.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = !wsManager.isConnected()
            ) {
                Text("Connect")
            }
            Button(
                onClick = { wsManager.disconnect() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = wsManager.isConnected()
            ) {
                Text("Disconnect")
            }
        }

        // Message Input
        TextField(
            value = messageInput.value,
            onValueChange = { messageInput.value = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            enabled = wsManager.isConnected()
        )

        Button(
            onClick = {
                if (messageInput.value.isNotEmpty()) {
                    val message = wsHandler.createMessage(messageInput.value)
                    wsManager.sendWebSocketMessage(message)
                    messageInput.value = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = wsManager.isConnected()
        ) {
            Text("Send Message")
        }

        // Messages List
        Text(
            text = "Messages (${messages.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(messages) { message ->
                MessageItem(message)
            }
        }
    }
}

@Composable
fun MessageItem(message: WebSocketMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Type: ${message.type}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = message.data,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return format.format(Date(timestamp))
}

