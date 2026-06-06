package name.faerytea.chat.data.api

import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body body: Map<String, String>): Response<String>

    @POST("logout")
    suspend fun logout(@Header("X-Auth-Token") token: String): Response<Unit>

    @GET("channels")
    suspend fun getChannels(@Header("X-Auth-Token") token: String): Response<List<String>>

    @GET("channel/{name}")
    suspend fun getChannelMessages(
        @Header("X-Auth-Token") token: String,
        @Path("name") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false,
    ): Response<List<Message>>

    @GET("inbox/{username}")
    suspend fun getInbox(
        @Header("X-Auth-Token") token: String,
        @Path("username") username: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false,
    ): Response<List<Message>>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: SendMessageRequest,
    ): Response<String>
}
