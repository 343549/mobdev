package name.faerytea.chat.data.repository

import name.faerytea.chat.data.api.ChatApi
import name.faerytea.chat.data.api.WebSocketManager
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageData
import name.faerytea.chat.data.model.SendMessageRequest

class ChatRepository(
    private val api: ChatApi,
    private val webSocketManager: WebSocketManager,
    private val authPreferences: AuthPreferences,
) {

    val newMessages = webSocketManager.newMessages

    suspend fun login(username: String, password: String): ApiResult<String> {
        val result = safeApiCall {
            api.login(mapOf("name" to username, "pwd" to password))
        }
        if (result is ApiResult.Success) {
            val rawToken = result.data
            // Response is plain text like: password: 'thetoken'
            // Actually for login it returns the token directly as plain text
            val token = rawToken.trim().removePrefix("password: '").removeSuffix("'").trim()
            authPreferences.saveCredentials(username, password)
            authPreferences.saveToken(token)
            webSocketManager.connect(username, token)
            return ApiResult.Success(token)
        }
        return result
    }

    suspend fun logout(token: String) {
        try {
            api.logout(token)
        } catch (_: Exception) { /* best effort */ }
        webSocketManager.disconnect()
        authPreferences.clearAll()
    }

    suspend fun getChannels(token: String): ApiResult<List<String>> =
        safeApiCall { api.getChannels(token) }

    suspend fun getChannelMessages(
        token: String,
        channelName: String,
        limit: Int = 20,
        lastKnownId: String = "0",
        reverse: Boolean = false,
    ): ApiResult<List<Message>> =
        safeApiCall { api.getChannelMessages(token, channelName, limit, lastKnownId, reverse) }

    suspend fun sendTextMessage(
        token: String,
        from: String,
        to: String,
        text: String,
    ): ApiResult<String> =
        safeApiCall {
            api.sendMessage(
                token,
                SendMessageRequest(from = from, to = to, data = MessageData.Text(text))
            )
        }

    fun connectWebSocket(username: String, token: String) {
        webSocketManager.connect(username, token)
    }

    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }
}
