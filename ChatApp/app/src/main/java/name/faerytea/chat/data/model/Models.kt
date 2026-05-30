package name.faerytea.chat.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Сервер присылает data в виде:
// { "Text": { "text": "hello" } }
// { "Image": { "link": "path/to/img.jpg" } }
// Это НЕ стандартный kotlinx.serialization sealed class,
// поэтому пишем кастомный сериализатор.

@Serializable(with = MessageDataSerializer::class)
sealed class MessageData {
    data class Text(val text: String) : MessageData()
    data class Image(val link: String) : MessageData()
}

object MessageDataSerializer : KSerializer<MessageData> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("MessageData")

    override fun deserialize(decoder: Decoder): MessageData {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Only JSON supported")
        val obj = jsonDecoder.decodeJsonElement().jsonObject

        return when {
            obj.containsKey("Text") -> {
                val text = obj["Text"]!!.jsonObject["text"]!!.jsonPrimitive.content
                MessageData.Text(text)
            }
            obj.containsKey("Image") -> {
                val link = obj["Image"]!!.jsonObject["link"]?.jsonPrimitive?.content ?: ""
                MessageData.Image(link)
            }
            else -> throw SerializationException("Unknown MessageData type: ${obj.keys}")
        }
    }

    override fun serialize(encoder: Encoder, value: MessageData) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Only JSON supported")
        val obj = when (value) {
            is MessageData.Text -> buildJsonObject {
                putJsonObject("Text") { put("text", value.text) }
            }
            is MessageData.Image -> buildJsonObject {
                putJsonObject("Image") { put("link", value.link) }
            }
        }
        jsonEncoder.encodeJsonElement(obj)
    }
}

@Serializable
data class Message(
    val id: String = "",
    val from: String = "",
    val to: String = "1@channel",
    val data: MessageData,
    val time: String = "",
)

@Serializable
data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: MessageData,
)
