package name.faerytea.chat.ui.messages

import name.faerytea.chat.data.model.Message

data class MessagesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
    val unauthorizedError: Boolean = false,
    val hasMore: Boolean = true,
    val sendError: String? = null,
    val isNetworkAvailable: Boolean = true,
    val isSendingDrafts: Boolean = false,
)
