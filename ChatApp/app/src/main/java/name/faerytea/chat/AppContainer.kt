package name.faerytea.chat

import android.content.Context
import name.faerytea.chat.data.api.NetworkModule
import name.faerytea.chat.data.api.WebSocketManager
import name.faerytea.chat.data.repository.AuthPreferences
import name.faerytea.chat.data.repository.ChatRepository

class AppContainer(context: Context) {
    val authPreferences = AuthPreferences(context)
    private val webSocketManager = WebSocketManager(NetworkModule.okHttpClient)
    val chatRepository = ChatRepository(
        api = NetworkModule.chatApi,
        webSocketManager = webSocketManager,
        authPreferences = authPreferences,
    )
}
