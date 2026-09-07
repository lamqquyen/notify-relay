package com.notifyrelay.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notifyrelay.app.RelayViewModel
import com.notifyrelay.app.data.ForwardedItem
import com.notifyrelay.app.util.isIgnoringBatteryOptimizations
import com.notifyrelay.app.util.isSamsungDevice
import com.notifyrelay.app.util.openAppDetailsSettings
import com.notifyrelay.app.util.openBatteryOptimizationSettings
import com.notifyrelay.app.util.openNotificationAccessSettings
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RelayViewModel,
    onOpenTelegram: () -> Unit,
    onOpenApps: () -> Unit,
    snackbarHost: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val hasAccess by viewModel.hasNotificationAccess.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val batteryOk = isIgnoringBatteryOptimizations(context)

    Scaffold(
        topBar = { TopAppBar(title = { Text("NotifyRelay") }) },
        snackbarHost = snackbarHost,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Forward selected Samsung notifications to Telegram on your iPhone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )

            StatusCard(
                title = "Notification access",
                body = if (hasAccess) {
                    "NotifyRelay can read notifications on this phone."
                } else {
                    "Required. Enable NotifyRelay in Android notification access."
                },
                checked = hasAccess,
                actionLabel = if (hasAccess) "Recheck" else "Enable",
                onAction = {
                    if (!hasAccess) openNotificationAccessSettings(context)
                    viewModel.refreshSystemState()
                },
                icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
            )

            StatusCard(
                title = "Telegram",
                body = if (settings.telegramConfigured) {
                    "Messages will go to chat ${settings.chatId}."
                } else {
                    "Add a bot token and chat ID. This is how the iPhone receives alerts."
                },
                checked = settings.telegramConfigured,
                actionLabel = if (settings.telegramConfigured) "Edit" else "Set up",
                onAction = onOpenTelegram,
                icon = { Icon(Icons.Outlined.Send, contentDescription = null) },
            )

            StatusCard(
                title = "Apps to forward",
                body = if (settings.allowedPackages.isEmpty()) {
                    "Nothing is forwarded until you pick apps."
                } else {
                    "${settings.allowedPackages.size} app${if (settings.allowedPackages.size == 1) "" else "s"} selected."
                },
                checked = settings.allowedPackages.isNotEmpty(),
                actionLabel = "Choose apps",
                onAction = onOpenApps,
                icon = { Icon(Icons.Outlined.Apps, contentDescription = null) },
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Forwarding", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (settings.canForward && hasAccess) {
                                "On. Only selected apps are sent to Telegram."
                            } else {
                                "Off until access, Telegram, and at least one app are ready."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = settings.forwardingEnabled && settings.canForward && hasAccess,
                        onCheckedChange = { viewModel.setForwarding(it) },
                    )
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Filters", fontWeight = FontWeight.SemiBold)
                    ToggleRow(
                        title = "Ignore ongoing notifications",
                        subtitle = "Skips music, navigation, charging, and other persistent items.",
                        checked = settings.ignoreOngoing,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(ignoreOngoing = checked) }
                        },
                    )
                    ToggleRow(
                        title = "Ignore group summaries",
                        subtitle = "Avoids duplicate bundled notifications from apps like Gmail.",
                        checked = settings.ignoreGroupSummaries,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(ignoreGroupSummaries = checked) }
                        },
                    )
                    OutlinedTextField(
                        value = settings.keywordInclude,
                        onValueChange = { value ->
                            viewModel.updateSettings { it.copy(keywordInclude = value) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Only if text contains (optional)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = settings.keywordExclude,
                        onValueChange = { value ->
                            viewModel.updateSettings { it.copy(keywordExclude = value) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Skip if text contains (optional)") },
                        singleLine = true,
                    )
                }
            }

            if (isSamsungDevice() || !batteryOk) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.BatterySaver, contentDescription = null)
                            Text(
                                "  Keep Samsung from killing this app",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            "Set NotifyRelay battery usage to Unrestricted, allow background activity, and lock the app in Recents. One UI will otherwise stop forwarding.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openBatteryOptimizationSettings(context) }) {
                                Text(if (batteryOk) "Battery already unrestricted" else "Allow unrestricted battery")
                            }
                        }
                        TextButton(onClick = { openAppDetailsSettings(context) }) {
                            Text("Open app settings")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.sendTestMessage() }, enabled = !busy) {
                    Text("Send test to iPhone")
                }
                OutlinedButton(onClick = { viewModel.refreshSystemState() }) {
                    Text("Refresh")
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent forwards", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (logs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearLogs() }) { Text("Clear") }
                        }
                    }
                    if (logs.isEmpty()) {
                        Text(
                            "Nothing forwarded yet. After setup, trigger one of the selected apps.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        logs.forEach { item -> LogRow(item) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    checked: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text("  $title", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    if (checked) "Ready" else "Needed",
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(body, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LogRow(item: ForwardedItem) {
    val time = DateFormat.format("HH:mm", Date(item.atMillis))
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "$time  ${item.appLabel}${if (item.success) "" else "  failed"}",
            fontWeight = FontWeight.Medium,
            color = if (item.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        val detail = listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" — ")
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
        if (!item.error.isNullOrBlank()) {
            Text(item.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
