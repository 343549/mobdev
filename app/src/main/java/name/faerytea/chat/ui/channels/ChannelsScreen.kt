package name.faerytea.chat.ui.channels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.faerytea.chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel,
    selectedChannel: String?,
    onChannelSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit,
    showTopBar: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.unauthorizedError) {
        if (uiState.unauthorizedError) onUnauthorized()
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_channels)) },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.action_sign_out),
                            )
                        }
                    },
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.channels, key = {item -> item}) { channel ->
                        val isSelected = channel == selectedChannel
                        ListItem(
                            headlineContent = { Text(text = channel) },
                            colors = if (isSelected) {
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                ListItemDefaults.colors()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelSelected(channel) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
