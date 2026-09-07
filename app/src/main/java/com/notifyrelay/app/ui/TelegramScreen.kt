package com.notifyrelay.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.notifyrelay.app.RelayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramScreen(
    viewModel: RelayViewModel,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val busy by viewModel.busy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telegram") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Create a private bot, then use it as the inbox on your iPhone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "1. On the iPhone, open Telegram and chat with @BotFather.\n" +
                    "2. Send /newbot, follow the prompts, and copy the token.\n" +
                    "3. Open your new bot and tap Start (or send any message).\n" +
                    "4. Paste the token below and tap Detect chat ID.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = settings.botToken,
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(botToken = value) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bot token") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            OutlinedTextField(
                value = settings.chatId,
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(chatId = value) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Chat ID") },
                singleLine = true,
            )
            OutlinedButton(
                onClick = { viewModel.detectChatId() },
                enabled = !busy && settings.botToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Detect chat ID")
            }
            Button(
                onClick = { viewModel.sendTestMessage() },
                enabled = !busy && settings.telegramConfigured,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send test message")
            }
            Text(
                "Do not forward banking, authenticator, or SMS OTP apps. Telegram bots are not end-to-end encrypted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
