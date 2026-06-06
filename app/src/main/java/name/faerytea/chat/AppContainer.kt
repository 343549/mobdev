package name.faerytea.chat

import android.content.Context
import name.faerytea.chat.data.api.NetworkModule
import name.faerytea.chat.data.api.WebSocketManager
import name.faerytea.chat.data.db.AppDatabase
import name.faerytea.chat.data.repository.AuthPreferences
import name.faerytea.chat.data.repository.ChatRepository
import name.faerytea.chat.util.NetworkMonitor


class AppContainer(context: Context) {
    val authPreferences = AuthPreferences(context)

    private val webSocketManager = WebSocketManager(NetworkModule.okHttpClient)

    val appDatabase = AppDatabase.getDatabase(context)

    val networkMonitor = NetworkMonitor(context)

    val chatRepository = ChatRepository(
        api = NetworkModule.chatApi,
        webSocketManager = webSocketManager,
        authPreferences = authPreferences,
        chatDao = appDatabase.chatDao(),
        networkMonitor = networkMonitor,
    )
}
