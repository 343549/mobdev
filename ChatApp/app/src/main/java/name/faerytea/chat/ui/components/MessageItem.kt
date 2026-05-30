package name.faerytea.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import name.faerytea.chat.R
import name.faerytea.chat.data.api.NetworkModule
import name.faerytea.chat.data.model.Message
import name.faerytea.chat.data.model.MessageData

@Composable
fun MessageItem(
    message: Message,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = message.from,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        when (val data = message.data) {
            is MessageData.Text -> {
                Text(
                    text = data.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            is MessageData.Image -> {
                val thumbUrl = "${NetworkModule.BASE_URL}thumb/${data.link}"
                val fullUrl = "${NetworkModule.BASE_URL}img/${data.link}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.content_description_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .padding(top = 4.dp)
                        .clickable { onImageClick(fullUrl) },
                )
            }
        }
    }
}
