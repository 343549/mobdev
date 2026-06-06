package name.faerytea.chat.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.faerytea.chat.data.repository.ApiResult
import name.faerytea.chat.data.repository.AuthPreferences
import name.faerytea.chat.data.repository.ChatRepository

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val token: String = "",
    val username: String = "",
    val error: String? = null,
)
class LoginViewModel(
    private val repository: ChatRepository,
    private val authPreferences: AuthPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState
    init {
        checkSavedCredentials()
    }

    private fun checkSavedCredentials() {
        viewModelScope.launch {
            val credentials = authPreferences.savedCredentials.first()
            val savedToken = authPreferences.savedToken.first()

            if (credentials != null && savedToken != null) {
                if (!repository.isNetworkAvailable.value) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            token = savedToken,
                            username = credentials.username,
                            error = null
                        )
                    }
                } else {
                    performLogin(credentials.username, credentials.password)
                }
            }
        }
    }


    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Введите логин и пароль") }
            return
        }
        performLogin(username, password)
    }

    private fun performLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.login(username, password)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        token = result.data,
                        username = username,
                        error = null
                    )
                }

                is ApiResult.Unauthorized -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Неверный логин или пароль"
                    )
                }

                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }



    fun clearError() = _uiState.update { it.copy(error = null) }

    class Factory(
        private val repository: ChatRepository,
        private val authPreferences: AuthPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(repository, authPreferences) as T
    }
}