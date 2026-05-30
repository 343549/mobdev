package name.faerytea.chat.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageData
import name.faerytea.chat.data.repository.ApiResult
import name.faerytea.chat.data.repository.ChatRepository

data class MessagesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
    val unauthorizedError: Boolean = false,
    val hasMore: Boolean = true,
    val sendError: String? = null,
)

class MessagesViewModel(
    private val repository: ChatRepository,
    private val token: String,
    private val channelName: String,
    private val username: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState

    // Track oldest message id for pagination (loading older messages)
    private var oldestKnownId: String = "0"
    // Track newest message id for WebSocket deduplication
    private var newestKnownId: String = "0"

    init {
        loadInitialMessages()
        observeWebSocket()
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getChannelMessages(
                token = token,
                channelName = channelName,
                limit = 20,
                lastKnownId = "0",
                reverse = false,
            )) {
                is ApiResult.Success -> {
                    val msgs = result.data
                    if (msgs.isNotEmpty()) {
                        oldestKnownId = msgs.first().id
                        newestKnownId = msgs.last().id
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = msgs,
                            hasMore = msgs.size >= 20,
                        )
                    }
                }
                is ApiResult.Unauthorized -> _uiState.update {
                    it.copy(isLoading = false, unauthorizedError = true)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = repository.getChannelMessages(
                token = token,
                channelName = channelName,
                limit = 20,
                lastKnownId = oldestKnownId,
                reverse = true,
            )) {
                is ApiResult.Success -> {
                    val older = result.data
                    if (older.isNotEmpty()) {
                        oldestKnownId = older.last().id
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            messages = older.reversed() + state.messages,
                            hasMore = older.size >= 20,
                        )
                    }
                }
                is ApiResult.Unauthorized -> _uiState.update {
                    it.copy(isLoadingMore = false, unauthorizedError = true)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoadingMore = false, error = result.message)
                }
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            repository.newMessages.collect { message ->
                val target = message.to
                if (target == channelName || target == "$channelName@channel" ||
                    channelName == target
                ) {
                    // Deduplicate
                    if (message.id != "0" && message.id <= newestKnownId) return@collect
                    if (message.id > newestKnownId) newestKnownId = message.id
                    _uiState.update { state ->
                        state.copy(messages = state.messages + message)
                    }
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
                    // Message will arrive via WebSocket or we add it manually as fallback
                    val optimistic = Message(
                        id = result.data,
                        from = username,
                        to = channelName,
                        data = MessageData.Text(text),
                    )
                    if (optimistic.id > newestKnownId) newestKnownId = optimistic.id
                    _uiState.update { state ->
                        val alreadyPresent = state.messages.any { it.id == optimistic.id }
                        if (alreadyPresent) state
                        else state.copy(messages = state.messages + optimistic)
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
