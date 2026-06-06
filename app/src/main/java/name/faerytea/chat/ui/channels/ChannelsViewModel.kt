package name.faerytea.chat.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.faerytea.chat.data.repository.ApiResult
import name.faerytea.chat.data.repository.ChatRepository

data class ChannelsUiState(
    val isLoading: Boolean = false,
    val channels: List<String> = emptyList(),
    val error: String? = null,
    val unauthorizedError: Boolean = false,
)

class ChannelsViewModel(
    private val repository: ChatRepository,
    private val token: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState
    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getChannels(token)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, channels = result.data)
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

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout(token)
            onLoggedOut()
        }
    }
    fun clearError() = _uiState.update { it.copy(error = null) }
    class Factory(
        private val repository: ChatRepository,
        private val token: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChannelsViewModel(repository, token) as T
    }
}
