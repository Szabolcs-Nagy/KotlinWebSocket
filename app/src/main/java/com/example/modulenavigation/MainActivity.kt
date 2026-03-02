package com.example.modulenavigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.modulenavigation.ui.theme.KotlinWebSocketTheme
import com.example.modulenavigation.ui.websocket.WebSocketScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinWebSocketTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    WebSocketScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KotlinWebSocketTheme {
        WebSocketScreen()
    }
}