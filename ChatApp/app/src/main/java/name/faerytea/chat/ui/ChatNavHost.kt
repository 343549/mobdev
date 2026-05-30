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

        // ── Экран входа ──────────────────────────────────────────────────────
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
            // На экране логина "Назад" = закрыть приложение (стандартное поведение системы)
            // BackHandler не нужен — пусть система сама закрывает
        }

        // ── Экран каналов (и landscape) ───────────────────────────────────────
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
                // Portrait: список каналов
                // "Назад" из списка каналов = закрыть приложение
                // BackHandler(enabled=true) с no-op перехватывает жест и не даёт выйти назад
                // на экран логина (которого уже нет в стеке), но тогда нельзя закрыть приложение.
                // Правильнее: НЕ перехватывать — стек пуст (логин удалён), система закроет приложение.
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

        // ── Экран сообщений (только portrait) ────────────────────────────────
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
                // Повернули во время просмотра сообщений в portrait →
                // перенаправляем на Channels, там LandscapeLayout покажет оба экрана
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
                // "Назад" из сообщений = вернуться в список каналов (popBackStack)
                // Это уже делает navigation compose автоматически через кнопку в TopAppBar
                // и системный жест/кнопку (т.к. в стеке есть Channels)
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

        // ── Полный экран картинки ─────────────────────────────────────────────
        composable(
            route = Screen.FullImage.route,
            arguments = listOf(navArgument("imageUrl") { type = NavType.StringType }),
        ) { backStackEntry ->
            val imageUrl = Uri.decode(backStackEntry.arguments?.getString("imageUrl") ?: "")
            // "Назад" = закрыть картинку = popBackStack (возврат в Messages)
            // Работает и системным жестом/кнопкой (стек содержит Messages),
            // и кнопкой Close в UI
            ImageScreen(
                imageUrl = imageUrl,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

// ── Landscape: список каналов слева + сообщения справа ───────────────────────

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
    // rememberSaveable — переживает поворот экрана
    var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }

    // "Назад" в landscape:
    // - если чат открыт → закрыть чат (selectedChannel = null)
    // - если чат закрыт → система закрывает приложение (enabled=false, не перехватываем)
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
            // Левая панель — список каналов
            ChannelsScreen(
                viewModel = channelsVm,
                selectedChannel = selectedChannel,
                onChannelSelected = { selectedChannel = it },
                onLogout = onLogout,
                onUnauthorized = onUnauthorized,
                showTopBar = false,
            )

            VerticalDivider()

            // Правая панель — сообщения или подсказка
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
                // key=channel: при смене канала создаётся новый ViewModel
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
                    showBackButton = false, // в landscape нет кнопки назад в чате
                )
            }
        }
    }
}
