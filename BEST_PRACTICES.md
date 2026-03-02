# WebSocket Best Practices Guide

## 🎯 Core Principles

### 1. **Lifecycle Management**

Always tie WebSocket lifecycle to Activity/Fragment lifecycle:

```kotlin
class MyActivity : ComponentActivity() {
    private val wsManager: WebSocketManager = get()
    private val listener = MyWebSocketListener()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wsManager.addListener(listener)
    }
    
    override fun onStart() {
        super.onStart()
        if (!wsManager.isConnected()) {
            wsManager.connect("wss://your-server.com/ws")
        }
    }
    
    override fun onStop() {
        super.onStop()
        wsManager.disconnect()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        wsManager.removeListener(listener)
    }
}
```

### 2. **Error Handling & Reconnection**

Implement resilient connection strategy:

```kotlin
private val reconnectAttempts = mutableStateOf(0)
private val maxReconnectAttempts = 5
private val reconnectDelay = 2000L // 2 seconds

override fun onError(error: Throwable) {
    Log.e("WS", "Connection error", error)
    
    if (reconnectAttempts.value < maxReconnectAttempts) {
        reconnectAttempts.value++
        scheduleReconnect()
    } else {
        showErrorToUser("Connection failed. Please try again later.")
    }
}

override fun onDisconnected(reason: String?) {
    Log.d("WS", "Disconnected: $reason")
    if (shouldReconnect) {
        scheduleReconnect()
    }
}

private fun scheduleReconnect() {
    Thread {
        Thread.sleep(reconnectDelay)
        if (!wsManager.isConnected()) {
            wsManager.connect(serverUrl)
        }
    }.start()
}
```

### 3. **Message Processing**

Keep message handlers lightweight and use coroutines for heavy work:

```kotlin
override fun onMessageReceived(message: WebSocketMessage) {
    when (message.type) {
        MessageType.MESSAGE -> {
            // Quick processing - update UI
            updateMessageList(message)
            
            // Heavy processing - use coroutine
            viewModelScope.launch(Dispatchers.Default) {
                val processedData = processMessageData(message.data)
                updateUIWithProcessedData(processedData)
            }
        }
        MessageType.ERROR -> handleError(message)
        else -> {}
    }
}

private suspend fun processMessageData(data: String): String {
    // Simulate heavy work
    delay(1000)
    return data.uppercase()
}
```

### 4. **Memory Leak Prevention**

```kotlin
// ❌ WRONG - Will cause memory leak
class MyListener : WebSocketListener {
    fun onCreate() {
        wsManager.addListener(this) // Reference kept forever
    }
}

// ✅ CORRECT - Cleanup properly
class MyActivity : ComponentActivity() {
    private val listener = MyListener()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wsManager.addListener(listener)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        wsManager.removeListener(listener) // Cleanup!
    }
}
```

### 5. **Concurrency & Thread Safety**

Use Koin with singletons for thread-safe access:

```kotlin
// Koin ensures single instance across app
val wsManager = koinInject<WebSocketManager>()

// All operations are internally synchronized
wsManager.addListener(listener1)  // Thread-safe
wsManager.addListener(listener2)  // Thread-safe
wsManager.sendMessage("text")     // Thread-safe
```

## 📊 Performance Optimization

### 1. **Message Batching**

For high-frequency messages, batch them:

```kotlin
private val messageBuffer = mutableListOf<WebSocketMessage>()
private val batchSize = 10
private val batchDelay = 1000L // milliseconds

override fun onMessageReceived(message: WebSocketMessage) {
    messageBuffer.add(message)
    
    if (messageBuffer.size >= batchSize) {
        processBatch()
    }
}

private fun processBatch() {
    val batch = messageBuffer.toList()
    messageBuffer.clear()
    
    viewModelScope.launch {
        // Process all messages at once
        updateUI(batch)
    }
}
```

### 2. **Connection Pooling**

Reuse connections efficiently:

```kotlin
// ✅ CORRECT - One global instance
val wsManager = koinInject<WebSocketManager>()
wsManager.connect(url1)

// Later, reuse for different operations
wsManager.sendMessage("command1")
wsManager.sendMessage("command2")

// ❌ WRONG - Multiple instances
val ws1 = WebSocketManager().also { it.connect(url) }
val ws2 = WebSocketManager().also { it.connect(url) }
```

### 3. **Memory-Efficient Message Handling**

```kotlin
// ❌ WRONG - Creates large lists
override fun onMessageReceived(message: WebSocketMessage) {
    allMessages.add(message) // Unbounded growth!
}

// ✅ CORRECT - Bounded buffer
private val messageQueue = Channel<WebSocketMessage>(Channel.BUFFERED)

init {
    viewModelScope.launch {
        for (message in messageQueue) {
            processMessage(message)
        }
    }
}

override fun onMessageReceived(message: WebSocketMessage) {
    viewModelScope.launch {
        messageQueue.send(message) // Respects buffer limit
    }
}
```

## 🔐 Security Best Practices

### 1. **Use Secure WebSocket (WSS)**

```kotlin
// ✅ CORRECT - Always use wss:// for production
wsManager.connect("wss://your-domain.com/ws")

// ❌ WRONG - Unencrypted connection
wsManager.connect("ws://your-domain.com/ws")
```

### 2. **Validate Server Certificates**

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(createSecureSSLSocketFactory(), trustManager)
    .certificatePinner(
        CertificatePinner.Builder()
            .add("your-domain.com", "sha256/AAAAAAAAAAAAAAAA...")
            .build()
    )
    .build()

val wsManager = WebSocketManager(okHttpClient)
```

### 3. **Authenticate Connections**

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        chain.proceed(request)
    }
    .build()

val wsManager = WebSocketManager(okHttpClient)
```

### 4. **Validate Message Format**

```kotlin
override fun onMessageReceived(message: WebSocketMessage) {
    try {
        // Validate message structure
        if (message.id.isEmpty()) throw IllegalArgumentException("Missing ID")
        if (message.data.isEmpty()) throw IllegalArgumentException("Missing data")
        
        // Process validated message
        processValidMessage(message)
    } catch (e: Exception) {
        Log.e("WS", "Invalid message received", e)
        // Discard and continue
    }
}
```

## 📈 Monitoring & Debugging

### 1. **Connection Health Monitoring**

```kotlin
private fun startHealthCheck() {
    viewModelScope.launch {
        while (isActive) {
            delay(30000) // Check every 30 seconds
            
            if (wsManager.isConnected()) {
                // Send ping
                wsManager.sendMessage("PING")
            } else {
                Log.w("WS", "Connection lost, attempting reconnect")
                wsManager.connect(serverUrl)
            }
        }
    }
}
```

### 2. **Message Logging**

```kotlin
override fun onMessageReceived(message: WebSocketMessage) {
    Log.d("WS", buildString {
        append("Received message: ")
        append("ID=${message.id}, ")
        append("Type=${message.type}, ")
        append("Size=${message.data.length}, ")
        append("Timestamp=${message.timestamp}")
    })
}
```

### 3. **Connection Metrics**

```kotlin
data class ConnectionMetrics(
    var connectedAt: Long = 0,
    var disconnectedAt: Long = 0,
    var messagesReceived: Int = 0,
    var messagesSent: Int = 0,
    var errorCount: Int = 0
) {
    val connectionDuration: Long
        get() = (disconnectedAt - connectedAt)
    
    val messagesPerSecond: Float
        get() = if (connectionDuration > 0) {
            (messagesReceived.toFloat() * 1000) / connectionDuration
        } else 0f
}

private val metrics = ConnectionMetrics()

override fun onConnected() {
    metrics.connectedAt = System.currentTimeMillis()
}

override fun onDisconnected(reason: String?) {
    metrics.disconnectedAt = System.currentTimeMillis()
    Log.d("WS", "Connection duration: ${metrics.connectionDuration}ms")
    Log.d("WS", "Messages/sec: ${metrics.messagesPerSecond}")
}

override fun onMessageReceived(message: WebSocketMessage) {
    metrics.messagesReceived++
}
```

## 🧪 Testing Best Practices

### 1. **Unit Testing Message Processing**

```kotlin
@Test
fun testMessageProcessing() {
    val message = WebSocketMessage(
        id = "test-1",
        type = MessageType.MESSAGE,
        data = "test data"
    )
    
    listener.onMessageReceived(message)
    
    // Verify message was processed
    verify(mockViewModel).updateUI(message)
}
```

### 2. **Mock WebSocket for Testing**

```kotlin
@Test
fun testConnectionFlow() {
    val mockWsManager = mock<WebSocketManager>()
    val listener = MyWebSocketListener()
    
    mockWsManager.addListener(listener)
    
    // Simulate connection
    listener.onConnected()
    listener.onMessageReceived(testMessage)
    listener.onDisconnected("Test")
    
    // Verify behavior
    verify(viewModel).onConnected()
    verify(viewModel).updateWithMessage(testMessage)
    verify(viewModel).onDisconnected()
}
```

## 🎯 Common Pitfalls to Avoid

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Not cleaning up listeners | Memory leak | Use `removeListener()` in onDestroy |
| Blocking the main thread | ANR (App Not Responding) | Use coroutines for heavy work |
| No error handling | Crashes on network issues | Implement all listener callbacks |
| Using ws:// in production | Security risk | Always use wss:// |
| Creating multiple WebSocketManager instances | Resource waste | Use singleton from Koin |
| Not validating incoming messages | App crashes or data corruption | Always validate message format |
| Ignoring connection state | Duplicate messages or lost data | Check `isConnected()` before sending |
| Unbounded message accumulation | Memory leak/overflow | Implement circular buffer or batch processing |

## 📋 Checklist for Production

- [ ] Use WSS (encrypted WebSocket)
- [ ] Implement proper error handling and reconnection
- [ ] Add connection timeout
- [ ] Validate all incoming messages
- [ ] Implement health checks/ping
- [ ] Monitor connection metrics
- [ ] Handle lifecycle properly
- [ ] Cleanup resources in onDestroy
- [ ] Test with unstable networks
- [ ] Test with slow/fast message rates
- [ ] Add logging for debugging
- [ ] Document custom message protocols
- [ ] Security review of authentication
- [ ] Performance testing with realistic data
- [ ] Unit and integration tests
