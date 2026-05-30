package name.faerytea.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import name.faerytea.chat.ui.ChatNavHost
import name.faerytea.chat.ui.theme.ChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as ChatApplication
        setContent {
            ChatTheme {
                ChatNavHost(application = application)
            }
        }
    }
}
