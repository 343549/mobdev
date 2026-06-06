package name.faerytea.chat.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageDataSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketManager(private val client: OkHttpClient) {

    private var webSocket: WebSocket? = null

    private val _newMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val newMessages: SharedFlow<Message> = _newMessages

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun connect(username: String, token: String) {
        val wsUrl = NetworkModule.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val url = "${wsUrl}ws/$username?token=$token"
        val request = Request.Builder().url(url).build()

        webSocket?.close(1000, null)

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = json.parseToJsonElement(text) as? JsonObject ?: return
                    val newMsgWrapper = root["NewMessage"] as? JsonObject ?: return
                    val msgElement = newMsgWrapper["msg"] ?: return

                    val msgObj = msgElement.jsonObject
                    val id   = msgObj["id"]?.let {
                        json.parseToJsonElement(it.toString()).toString().trim('"')
                    } ?: ""
                    val from = msgObj["from"]?.toString()?.trim('"') ?: ""
                    val to   = msgObj["to"]?.toString()?.trim('"') ?: ""
                    val time = msgObj["time"]?.toString()?.trim('"') ?: ""
                    val dataElement = msgObj["data"] ?: return

                    val data = json.decodeFromJsonElement(
                        MessageDataSerializer,
                        dataElement,
                    )

                    val msg = Message(id = id, from = from, to = to, data = data, time = time)
                    _newMessages.tryEmit(msg)
                } catch (_: Exception) {
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
    }
}
