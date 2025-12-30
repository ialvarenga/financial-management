package com.example.gerenciadorfinanceiro.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.util.openNotificationSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPermission: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Parser") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MasterSwitchCard(
                isEnabled = uiState.isEnabled,
                onToggle = { viewModel.toggleFeature(it) }
            )

            if (!uiState.isPermissionGranted) {
                PermissionWarningCard(
                    onOpenSettings = {
                        openNotificationSettings(context)
                        viewModel.refreshPermissionStatus()
                    }
                )
            }

            if (uiState.isEnabled) {
                SourceTogglesCard(
                    itauEnabled = uiState.itauEnabled,
                    nubankEnabled = uiState.nubankEnabled,
                    googleWalletEnabled = uiState.googleWalletEnabled,
                    onToggleItau = { viewModel.toggleSource(NotificationSource.ITAU, it) },
                    onToggleNubank = { viewModel.toggleSource(NotificationSource.NUBANK, it) },
                    onToggleWallet = { viewModel.toggleSource(NotificationSource.GOOGLE_WALLET, it) }
                )

                InfoCard()
            }
        }
    }
}

@Composable
private fun MasterSwitchCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Notification Parser",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Auto-create transactions from bank notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun PermissionWarningCard(
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "Notification access permission is required for this feature to work. Please enable it in settings.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun SourceTogglesCard(
    itauEnabled: Boolean,
    nubankEnabled: Boolean,
    googleWalletEnabled: Boolean,
    onToggleItau: (Boolean) -> Unit,
    onToggleNubank: (Boolean) -> Unit,
    onToggleWallet: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Monitored Sources",
                style = MaterialTheme.typography.titleMedium
            )

            SourceToggleRow(
                name = "Itaú",
                description = "PIX received/sent",
                enabled = itauEnabled,
                onToggle = onToggleItau
            )

            HorizontalDivider()

            SourceToggleRow(
                name = "Nubank",
                description = "Transfer received/sent",
                enabled = nubankEnabled,
                onToggle = onToggleNubank
            )

            HorizontalDivider()

            SourceToggleRow(
                name = "Google Wallet",
                description = "Credit card purchases",
                enabled = googleWalletEnabled,
                onToggle = onToggleWallet
            )
        }
    }
}

@Composable
private fun SourceToggleRow(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "• Auto-created transactions have PENDING status",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Review and confirm them in your transaction list",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Default category is 'Other' (you can change it)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Duplicate notifications are automatically ignored",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
