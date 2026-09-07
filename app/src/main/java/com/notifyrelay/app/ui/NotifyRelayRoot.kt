package com.notifyrelay.app.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.notifyrelay.app.RelayViewModel

private enum class Screen { Home, Telegram, Apps }

@Composable
fun NotifyRelayRoot(viewModel: RelayViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val snackbar = remember { SnackbarHostState() }
    val message by viewModel.statusMessage.collectAsState()

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.consumeStatusMessage()
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            viewModel = viewModel,
            onOpenTelegram = { screen = Screen.Telegram },
            onOpenApps = { screen = Screen.Apps },
            snackbarHost = { SnackbarHost(snackbar) },
        )
        Screen.Telegram -> TelegramScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.Home },
            snackbarHost = { SnackbarHost(snackbar) },
        )
        Screen.Apps -> AppPickerScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.Home },
        )
    }
}
