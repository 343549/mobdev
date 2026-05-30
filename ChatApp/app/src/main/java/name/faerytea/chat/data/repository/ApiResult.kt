package name.faerytea.chat.data.repository

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object Unauthorized : ApiResult<Nothing>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        val response = call()
        when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Error("Пустой ответ сервера")
            }
            response.code() == 401 -> ApiResult.Unauthorized
            else -> ApiResult.Error("Ошибка сервера: ${response.code()}")
        }
    } catch (e: Exception) {
        ApiResult.Error("Ошибка сети")
    }
}
