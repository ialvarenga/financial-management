package com.example.gerenciadorfinanceiro.ui.screens.mais

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.backup.BackupPreviewInfo
import com.example.gerenciadorfinanceiro.data.backup.ImportEntity
import com.example.gerenciadorfinanceiro.data.backup.ImportEntityFilter
import com.example.gerenciadorfinanceiro.ui.screens.settings.BackupSettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun generateBackupFileName(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    return "backup_${dateFormat.format(Date())}.json"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadBackupForPreview(it) }
    }

    LaunchedEffect(uiState.exportSuccess) {
        uiState.exportSuccess?.let { fileName ->
            snackbarHostState.showSnackbar("Backup exportado com sucesso: $fileName")
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.importSuccess) {
        uiState.importSuccess?.let { info ->
            val parts = buildList {
                if (info.accountCount > 0) add("${info.accountCount} contas")
                if (info.transactionCount > 0) add("${info.transactionCount} transações")
                if (info.creditCardCount > 0) add("${info.creditCardCount} cartões")
                if (info.recurrenceCount > 0) add("${info.recurrenceCount} recorrências")
                if (info.transferCount > 0) add("${info.transferCount} transferências")
                if (info.creditCardBillCount > 0) add("${info.creditCardBillCount} faturas")
                if (info.creditCardItemCount > 0) add("${info.creditCardItemCount} itens")
            }
            val message = "Importação concluída! " + parts.joinToString(", ")
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            snackbarHostState.showSnackbar("Todos os dados foram apagados.")
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    // Entity selection dialog shown after reading the backup file
    if (uiState.backupPreview != null) {
        ImportEntitySelectionDialog(
            preview = uiState.backupPreview!!,
            filter = uiState.entityFilter,
            onToggle = { entity, checked -> viewModel.toggleEntityFilter(entity, checked) },
            onConfirm = { viewModel.confirmImport() },
            onDismiss = { viewModel.cancelImport() }
        )
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Resetar Todos os Dados") },
            text = {
                Text(
                    "ATENÇÃO: Esta ação irá apagar permanentemente TODOS os dados do aplicativo, " +
                    "incluindo contas, transações, cartões, recorrências e transferências.\n\n" +
                    "Esta ação NÃO PODE ser desfeita. Deseja continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sim, Apagar Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ferramentas de Dados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Backup",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            ToolOption(
                title = "Exportar Dados",
                description = "Salvar todos os dados em arquivo JSON",
                icon = Icons.Default.Upload,
                isLoading = uiState.isExporting,
                onClick = { exportLauncher.launch(generateBackupFileName()) }
            )

            ToolOption(
                title = "Importar Dados",
                description = "Restaurar dados de um arquivo de backup",
                icon = Icons.Default.Download,
                isLoading = uiState.isImporting || uiState.isLoadingBackup,
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zona de Perigo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            ToolOption(
                title = "Resetar Todos os Dados",
                description = "Apagar permanentemente todos os dados do aplicativo",
                icon = Icons.Default.DeleteForever,
                isLoading = uiState.isResetting,
                isDestructive = true,
                onClick = { showResetDialog = true }
            )
        }
    }
}

@Composable
private fun ImportEntitySelectionDialog(
    preview: BackupPreviewInfo,
    filter: ImportEntityFilter,
    onToggle: (ImportEntity, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text("Importar Dados") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "Selecione os dados que deseja importar do backup:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Accounts
                EntityCheckboxRow(
                    label = "Contas",
                    count = preview.accountCount,
                    checked = filter.accounts,
                    enabled = true,
                    onCheckedChange = { onToggle(ImportEntity.ACCOUNTS, it) }
                )
                // Transactions (depends on accounts)
                EntityCheckboxRow(
                    label = "Transações",
                    count = preview.transactionCount,
                    checked = filter.transactions,
                    enabled = filter.accounts,
                    indent = true,
                    onCheckedChange = { onToggle(ImportEntity.TRANSACTIONS, it) }
                )
                // Transfers (depends on accounts)
                EntityCheckboxRow(
                    label = "Transferências",
                    count = preview.transferCount,
                    checked = filter.transfers,
                    enabled = filter.accounts,
                    indent = true,
                    onCheckedChange = { onToggle(ImportEntity.TRANSFERS, it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Credit cards
                EntityCheckboxRow(
                    label = "Cartões de Crédito",
                    count = preview.creditCardCount,
                    checked = filter.creditCards,
                    enabled = true,
                    onCheckedChange = { onToggle(ImportEntity.CREDIT_CARDS, it) }
                )
                // Bills (depends on credit cards)
                EntityCheckboxRow(
                    label = "Faturas",
                    count = preview.creditCardBillCount,
                    checked = filter.creditCardBills,
                    enabled = filter.creditCards,
                    indent = true,
                    onCheckedChange = { onToggle(ImportEntity.CREDIT_CARD_BILLS, it) }
                )
                // Items (depends on bills)
                EntityCheckboxRow(
                    label = "Itens das Faturas",
                    count = preview.creditCardItemCount,
                    checked = filter.creditCardItems,
                    enabled = filter.creditCards && filter.creditCardBills,
                    indent = true,
                    onCheckedChange = { onToggle(ImportEntity.CREDIT_CARD_ITEMS, it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Recurrences (independent)
                EntityCheckboxRow(
                    label = "Recorrências",
                    count = preview.recurrenceCount,
                    checked = filter.recurrences,
                    enabled = true,
                    onCheckedChange = { onToggle(ImportEntity.RECURRENCES, it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "⚠ Todos os dados existentes serão substituídos pelos selecionados. Esta ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EntityCheckboxRow(
    label: String,
    count: Int,
    checked: Boolean,
    enabled: Boolean,
    indent: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 24.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled
        )
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        shape = MaterialTheme.shapes.medium,
        color = if (isDestructive)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
