package com.example.gerenciadorfinanceiro.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.ui.components.CurrencyTextField
import com.example.gerenciadorfinanceiro.util.toReais

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val amountDigits by viewModel.amountDigits.collectAsState()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(isPostNotificationsGranted(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        hasNotificationPermission = isPostNotificationsGranted(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orçamento do Cartão") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
            EnableBudgetCard(
                isEnabled = uiState.enabled,
                onToggle = { enabled ->
                    viewModel.setEnabled(enabled)
                    if (enabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasNotificationPermission
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            if (uiState.enabled && !hasNotificationPermission) {
                NotificationPermissionWarningCard(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                Settings.EXTRA_APP_PACKAGE, context.packageName
                            )
                        )
                    }
                )
            }

            if (uiState.enabled) {
                CurrencyTextField(
                    valueDigits = amountDigits,
                    onValueChange = { viewModel.onAmountChange(it) },
                    label = { Text("Orçamento mensal") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.budgetCents > 0) {
                    BudgetProgressCard(
                        spendCents = uiState.spendCents,
                        budgetCents = uiState.budgetCents,
                        percentage = uiState.percentage
                    )
                }

                BudgetInfoCard()
            }
        }
    }
}

private fun isPostNotificationsGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun EnableBudgetCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ativar orçamento mensal",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Receba broncas quando os gastos do cartão se aproximarem do limite",
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
private fun NotificationPermissionWarningCard(
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
                    text = "Notificações bloqueadas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "Sem a permissão de notificações os alertas de orçamento não vão aparecer.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir configurações")
            }
        }
    }
}

@Composable
private fun BudgetProgressCard(
    spendCents: Long,
    budgetCents: Long,
    percentage: Float
) {
    val overBudget = spendCents > budgetCents
    val progressColor = if (overBudget) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Gastos das faturas em aberto",
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                color = progressColor,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${spendCents.toReais()} de ${budgetCents.toReais()} (${percentage.toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (overBudget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (overBudget) {
                Text(
                    text = "Orçamento estourado. As broncas matinais estão a caminho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BudgetInfoCard() {
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
                    text = "Como funciona",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "• Conta os gastos das faturas em aberto, somando todos os cartões",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Avisos (nada gentis) ao atingir 80% e 90% do orçamento",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Passou do orçamento? Bronca na hora e todo dia de manhã às 8h",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Fechou a fatura? Ela sai da conta e o orçamento recomeça nas faturas seguintes",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Cada aviso de limite aparece só uma vez por ciclo de fatura",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
