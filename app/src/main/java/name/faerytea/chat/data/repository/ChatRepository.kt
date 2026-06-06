package name.faerytea.chat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import name.faerytea.chat.data.api.ChatApi
import name.faerytea.chat.data.api.WebSocketManager
import name.faerytea.chat.data.db.CachedChannel
import name.faerytea.chat.data.db.CachedMessage
import name.faerytea.chat.data.db.ChatDao
import name.faerytea.chat.data.db.DraftMessage
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageData
import name.faerytea.chat.data.model.SendMessageRequest
import name.faerytea.chat.util.NetworkMonitor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatRepository(
    private val api: ChatApi,
    private val webSocketManager: WebSocketManager,
    private val authPreferences: AuthPreferences,
    private val chatDao: ChatDao,
    private val networkMonitor: NetworkMonitor,
) {

    val newMessages = webSocketManager.newMessages
    val isNetworkAvailable = networkMonitor.isNetworkAvailable

    suspend fun login(username: String, password: String): ApiResult<String> {
        val result = safeApiCall {
            api.login(mapOf("name" to username, "pwd" to password))
        }
        if (result is ApiResult.Success) {
            val token = result.data.trim()
            authPreferences.saveCredentials(username, password)
            authPreferences.saveToken(token)
            webSocketManager.connect(username, token)
            return ApiResult.Success(token)
        }
        return result
    }
    suspend fun logout(token: String) {
        runCatching { api.logout(token) }
        webSocketManager.disconnect()
        authPreferences.clearAll()
        chatDao.deleteAllChannels()
    }


    suspend fun getChannels(token: String): ApiResult<List<String>> {
        if (!networkMonitor.isNetworkAvailable.value) {
            val cached = chatDao.getCachedChannels()
            return if (cached.isNotEmpty())
                ApiResult.Success(cached)
            else
                ApiResult.Error("Нет кэшированных каналов")
        }

        return when (val result = safeApiCall { api.getChannels(token) }) {
            is ApiResult.Success -> {
                chatDao.deleteAllChannels()
                chatDao.insertChannels(
                    result.data.map { CachedChannel(it, System.currentTimeMillis()) }
                )
                ApiResult.Success(result.data)
            }
            else -> {
                val cached = chatDao.getCachedChannels()
                if (cached.isNotEmpty())
                    ApiResult.Success(cached)
                else
                    result
            }
        }
    }

    fun getChannelMessages(
        channelName: String,
    ): Flow<List<CachedMessage>> = chatDao.getMessagesForChannel(channelName)

    suspend fun loadChannelRemoteMessages(
        token: String,
        channelName: String,
        limit: Int = 100,
        lastKnownId: String = "0",
        reverse: Boolean = false,
    ) {
        when (val result = safeApiCall {
            api.getChannelMessages(token, channelName, limit, lastKnownId, reverse)
        }) {
            is ApiResult.Success -> {
                result.data.forEach { msg ->
                    chatDao.insertMessage(
                        CachedMessage(
                            id = msg.id,
                            channelName = channelName,
                            from = msg.from,
                            to = msg.to,
                            text = if (msg.data is MessageData.Text) (msg.data as MessageData.Text).text else "",
                            imageLink = if (msg.data is MessageData.Image) (msg.data as MessageData.Image).link else "",
                            isImage = msg.data is MessageData.Image,
                            timestamp = System.currentTimeMillis(),
                            isFromNetwork = true,
                        )
                    )
                }
                ApiResult.Success(result.data)
            }
            is ApiResult.Unauthorized -> ApiResult.Unauthorized
            is ApiResult.Error -> {
                val cached = chatDao.getMessagesForChannel(channelName, limit)
                if (cached.isNotEmpty())
                    ApiResult.Success(cached.map { it.toMessage() })
                else
                    result
            }
        }
    }


    suspend fun getChannelMessages(
        token: String,
        channelName: String,
        limit: Int = 20,
        lastKnownId: String = "0",
        reverse: Boolean = false,
    ): ApiResult<List<Message>> {

        if (!networkMonitor.isNetworkAvailable.value) {
            val cached = chatDao.getMessagesForChannel(channelName, limit)
            return if (cached.isNotEmpty())
                ApiResult.Success(cached.map { it.toMessage() })
            else
                ApiResult.Error("Нет сообщений в кэше")
        }

        return when (val result = safeApiCall {
            api.getChannelMessages(token, channelName, limit, lastKnownId, reverse)
        }) {
            is ApiResult.Success -> {
                result.data.forEach { msg ->
                    chatDao.insertMessage(
                        CachedMessage(
                            id = msg.id,
                            channelName = channelName,
                            from = msg.from,
                            to = msg.to,
                            text = if (msg.data is MessageData.Text) (msg.data as MessageData.Text).text else "",
                            imageLink = if (msg.data is MessageData.Image) (msg.data as MessageData.Image).link else "",
                            isImage = msg.data is MessageData.Image,
                            timestamp = System.currentTimeMillis(),
                            isFromNetwork = true,
                        )
                    )
                }
                ApiResult.Success(result.data)
            }
            is ApiResult.Unauthorized -> ApiResult.Unauthorized
            is ApiResult.Error -> {
                val cached = chatDao.getMessagesForChannel(channelName, limit)
                if (cached.isNotEmpty())
                    ApiResult.Success(cached.map { it.toMessage() })
                else
                    result
            }
        }
    }


    fun getMessagesForChannelFlow(channelName: String): Flow<List<Message>> {
        return chatDao.getMessagesForChannelFlow(channelName).map { cached ->
            cached.map { it.toMessage() }
        }
    }


    suspend fun sendTextMessage(
        token: String,
        from: String,
        to: String,
        text: String,
    ): ApiResult<String> {

        if (!networkMonitor.isNetworkAvailable.value) {
            val draftId = chatDao.insertDraft(
                DraftMessage(
                    channelName = to,
                    from = from,
                    text = text,
                    createdAt = System.currentTimeMillis(),
                )
            )
            chatDao.insertMessage(
                CachedMessage(
                    id = draftId.toString(),
                    channelName = to,
                    from = from,
                    to = to,
                    text = text,
                    isImage = false,
                    timestamp = System.currentTimeMillis(),
                    isFromNetwork = false,  // это наше сообщение
                    isPending = true
                )
            )
            return ApiResult.Success("draft_$draftId")  // возвращаем фиктивный ID
        }

        return when (val result = safeApiCall {
            api.sendMessage(
                token,
                SendMessageRequest(from = from, to = to, data = MessageData.Text(text))
            )
        }) {
            is ApiResult.Success -> {
                chatDao.insertMessage(
                    CachedMessage(
                        id = result.data,
                        channelName = to,
                        from = from,
                        to = to,
                        text = text,
                        isImage = false,
                        timestamp = System.currentTimeMillis(),
                        isFromNetwork = false,
                    )
                )
                result
            }
            else -> result
        }
    }

    suspend fun sendPendingDrafts(token: String, from: String) {
        val drafts = chatDao.getUnsentDrafts()
        drafts.forEach { draft ->
            try {
                chatDao.updateDraftAttempt(draft.id, System.currentTimeMillis())

                val result = safeApiCall {
                    api.sendMessage(
                        token,
                        SendMessageRequest(
                            from = from,
                            to = draft.channelName,
                            data = MessageData.Text(draft.text)
                        )
                    )
                }
                if (result is ApiResult.Success) {
                    chatDao.insertMessage(
                        CachedMessage(
                            id = result.data,
                            channelName = draft.channelName,
                            from = from,
                            to = draft.channelName,
                            text = draft.text,
                            isImage = false,
                            timestamp = System.currentTimeMillis(),
                            isFromNetwork = false,
                        )
                    )
                    chatDao.deleteMessageById(draft.id)
                    chatDao.deleteDraft(draft)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }
}

fun CachedMessage.toMessage(): Message {
    val data = if (isImage)
        MessageData.Image(imageLink)
    else
        MessageData.Text(text)
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")

    val dateString = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    return Message(
        id = id,
        from = from,
        to = to,
        data = data,
        time = dateString,
        isPending = isPending
    )
}
