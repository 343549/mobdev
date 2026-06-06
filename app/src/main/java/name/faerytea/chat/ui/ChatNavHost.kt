package name.faerytea.chat.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import name.faerytea.chat.ChatApplication
import name.faerytea.chat.R
import name.faerytea.chat.ui.channels.ChannelsScreen
import name.faerytea.chat.ui.channels.ChannelsViewModel
import name.faerytea.chat.ui.image.ImageScreen
import name.faerytea.chat.ui.login.LoginScreen
import name.faerytea.chat.ui.login.LoginViewModel
import name.faerytea.chat.ui.messages.MessagesScreen
import name.faerytea.chat.ui.messages.MessagesViewModel

@Composable
fun ChatNavHost(application: ChatApplication) {
    val navController = rememberNavController()
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(
                    repository = application.container.chatRepository,
                    authPreferences = application.container.authPreferences,
                )
            )
            LoginScreen(
                viewModel = vm,
                onLoggedIn = { token, username ->
                    navController.navigate(Screen.Channels.createRoute(token, username)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Channels.route,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val token = Uri.decode(backStackEntry.arguments?.getString("token") ?: "")
            val username = Uri.decode(backStackEntry.arguments?.getString("username") ?: "")

            val channelsVm: ChannelsViewModel = viewModel(
                factory = ChannelsViewModel.Factory(
                    repository = application.container.chatRepository,
                    token = token,
                )
            )

            if (isLandscape) {
                LandscapeLayout(
                    channelsVm = channelsVm,
                    token = token,
                    username = username,
                    application = application,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onUnauthorized = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onImageClick = { imageUrl ->
                        navController.navigate(Screen.FullImage.createRoute(imageUrl))
                    },
                )
            } else {
                ChannelsScreen(
                    viewModel = channelsVm,
                    selectedChannel = null,
                    onChannelSelected = { channel ->
                        navController.navigate(
                            Screen.Messages.createRoute(token, username, channel)
                        )
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onUnauthorized = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(
            route = Screen.Messages.route,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val token = Uri.decode(backStackEntry.arguments?.getString("token") ?: "")
            val username = Uri.decode(backStackEntry.arguments?.getString("username") ?: "")
            val channelName = Uri.decode(backStackEntry.arguments?.getString("channelName") ?: "")

            if (isLandscape) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Channels.createRoute(token, username)) {
                        popUpTo(Screen.Channels.route) { inclusive = true }
                    }
                }
            } else {
                val messagesVm: MessagesViewModel = viewModel(
                    factory = MessagesViewModel.Factory(
                        repository = application.container.chatRepository,
                        token = token,
                        channelName = channelName,
                        username = username,
                    )
                )
                MessagesScreen(
                    viewModel = messagesVm,
                    channelName = channelName,
                    onBack = { navController.popBackStack() },
                    onImageClick = { imageUrl ->
                        navController.navigate(Screen.FullImage.createRoute(imageUrl))
                    },
                    onUnauthorized = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    showBackButton = true,
                )
            }
        }
        composable(
            route = Screen.FullImage.route,
            arguments = listOf(navArgument("imageUrl") { type = NavType.StringType }),
        ) { backStackEntry ->
            val imageUrl = Uri.decode(backStackEntry.arguments?.getString("imageUrl") ?: "")
            ImageScreen(
                imageUrl = imageUrl,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeLayout(
    channelsVm: ChannelsViewModel,
    token: String,
    username: String,
    application: ChatApplication,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit,
    onImageClick: (String) -> Unit,
) {
    var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedChannel != null) {
        selectedChannel = null
    }

    Scaffold(
        topBar = {
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
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            ChannelsScreen(
                viewModel = channelsVm,
                selectedChannel = selectedChannel,
                onChannelSelected = { selectedChannel = it },
                onLogout = onLogout,
                onUnauthorized = onUnauthorized,
                showTopBar = false,
            )

            VerticalDivider()

            val channel = selectedChannel
            if (channel == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.title_select_chat),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                val messagesVm: MessagesViewModel = viewModel(
                    key = channel,
                    factory = MessagesViewModel.Factory(
                        repository = application.container.chatRepository,
                        token = token,
                        channelName = channel,
                        username = username,
                    )
                )
                MessagesScreen(
                    viewModel = messagesVm,
                    channelName = channel,
                    onBack = { selectedChannel = null },
                    onImageClick = onImageClick,
                    onUnauthorized = onUnauthorized,
                    showBackButton = false,
                )
            }
        }
    }
}
