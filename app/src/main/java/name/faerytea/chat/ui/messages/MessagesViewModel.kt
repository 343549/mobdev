package name.faerytea.chat.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.faerytea.chat.data.db.CachedMessage
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageData
import name.faerytea.chat.data.repository.ApiResult
import name.faerytea.chat.data.repository.ChatRepository
import name.faerytea.chat.data.repository.toMessage

class MessagesViewModel(
    private val repository: ChatRepository,
    private val token: String,
    private val channelName: String,
    private val username: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState

    private var oldestKnownId: String = "0"
    private var newestKnownId: String = "0"

    init {
        loadInitialRemoteMessages()
        observeChatMessageChanges()
        observeWebSocket()
        observeNetworkAndDrafts()
    }

//    Coroutines
//    Scope
//    launch - async - runBlocking
//    Context - Dispatchers (Main, IO), Job
    private fun observeNetworkAndDrafts() {
        viewModelScope.launch {
            // Комбинируем: когда сеть появляется, отправляем черновики
            repository.isNetworkAvailable.combine(
                MutableStateFlow(Unit)
            ) { isNetworkAvailable, _ ->
                _uiState.update { it.copy(isNetworkAvailable = isNetworkAvailable) }

                // Когда сеть появляется и не было ошибок — отправляем черновики
                if (isNetworkAvailable && !_uiState.value.unauthorizedError) {
                    sendPendingDrafts()
                }
            }.collect {}
        }
    }

    private fun observeChatMessageChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getChannelMessages(channelName)
                .collectLatest { messages: List<CachedMessage> ->
                if (messages.isNotEmpty()) {
                    oldestKnownId = messages.first().id
                    newestKnownId = messages.last().id
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = messages.map { message -> message.toMessage() },
                        hasMore = false,
                    )
                }
            }
        }
    }

    private fun loadInitialRemoteMessages() {
        viewModelScope.launch {
            repository.loadChannelRemoteMessages(token, channelName)
        }
    }


    private fun observeWebSocket() {
        viewModelScope.launch {
            repository.newMessages.collect { message ->
                val isForThisChannel = message.to == channelName ||
                                       message.to == "$channelName@channel"
                if (!isForThisChannel) return@collect

                if (message.id != "0" && message.id <= newestKnownId) return@collect

                if (message.id > newestKnownId) newestKnownId = message.id

                _uiState.update { state ->
                    state.copy(messages = state.messages + message)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(sendError = "Нельзя отправить пустое сообщение") }
            return
        }
        viewModelScope.launch {
            when (val result = repository.sendTextMessage(
                token = token,
                from = username,
                to = channelName,
                text = text,
            )) {
                is ApiResult.Success -> {
                    // Оптимистичное добавление или использование кэша
                    val optimistic = Message(
                        id = result.data,
                        from = username,
                        to = channelName,
                        data = MessageData.Text(text),
                    )
                    if (optimistic.id.startsWith("draft_")) {
                        _uiState.update { it.copy(sendError = "Сообщение сохранено, отправится при появлении сети") }
                    } else {
                        if (optimistic.id > newestKnownId) newestKnownId = optimistic.id
                        _uiState.update { state ->
                            val alreadyPresent = state.messages.any { it.id == optimistic.id }
                            if (alreadyPresent) state
                            else state.copy(messages = state.messages + optimistic)
                        }
                    }
                }
                is ApiResult.Unauthorized -> _uiState.update {
                    it.copy(unauthorizedError = true)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(sendError = result.message)
                }
            }
        }
    }

    private fun sendPendingDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingDrafts = true) }
            try {
                repository.sendPendingDrafts(token, username)
//                loadMoreMessages()
                loadInitialRemoteMessages()
            } catch (_: Exception) {

            }
            _uiState.update { it.copy(isSendingDrafts = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null, sendError = null) }

    class Factory(
        private val repository: ChatRepository,
        private val token: String,
        private val channelName: String,
        private val username: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(repository, token, channelName, username) as T
    }
}
