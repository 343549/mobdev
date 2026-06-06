package name.faerytea.chat.ui

import android.net.Uri

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Channels : Screen("channels/{token}/{username}") {
        fun createRoute(token: String, username: String): String =
            "channels/${Uri.encode(token)}/${Uri.encode(username)}"
    }
    data object Messages : Screen("messages/{token}/{username}/{channelName}") {
        fun createRoute(token: String, username: String, channelName: String): String =
            "messages/${Uri.encode(token)}/${Uri.encode(username)}/${Uri.encode(channelName)}"
    }
    data object FullImage : Screen("image/{imageUrl}") {
        fun createRoute(imageUrl: String): String = "image/${Uri.encode(imageUrl)}"
    }
}
