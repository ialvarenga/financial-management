package com.example.gerenciadorfinanceiro.ui.screens.mais

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.ui.screens.settings.BackupSettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun generateFileName(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    return "backup_${dateFormat.format(Date())}.json"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaisScreen(
    onNavigateToCreditCards: () -> Unit,
    onNavigateToRecurrences: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            showImportDialog = true
        }
    }

    LaunchedEffect(uiState.exportSuccess) {
        uiState.exportSuccess?.let { fileName ->
            snackbarHostState.showSnackbar("Backup exportado com sucesso: $fileName")
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.importSuccess) {
        uiState.importSuccess?.let { info ->
            val message = buildString {
                append("Importação concluída!\n")
                append("${info.accountCount} contas, ")
                append("${info.transactionCount} transações, ")
                append("${info.creditCardCount} cartões, ")
                append("${info.recurrenceCount} recorrências, ")
                append("${info.transferCount} transferências")
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportUri = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Confirmar Importação") },
            text = {
                Text(
                    "ATENÇÃO: Todos os dados existentes serão substituídos pelos dados do backup. " +
                    "Esta ação não pode ser desfeita.\n\n" +
                    "Deseja continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { viewModel.importBackup(it) }
                        showImportDialog = false
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sim, Substituir Dados")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        pendingImportUri = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mais") }
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
            MenuOption(
                title = "Cartões de Crédito",
                description = "Gerencie seus cartões e faturas",
                icon = Icons.Default.CreditCard,
                onClick = onNavigateToCreditCards
            )

            MenuOption(
                title = "Recorrências",
                description = "Transações recorrentes e agendamentos",
                icon = Icons.Default.Repeat,
                onClick = onNavigateToRecurrences
            )

            MenuOption(
                title = "Exportar Dados",
                description = "Salvar todos os dados em arquivo JSON",
                icon = Icons.Default.Upload,
                onClick = { exportLauncher.launch(generateFileName()) }
            )

            MenuOption(
                title = "Importar Dados",
                description = "Restaurar dados de um backup",
                icon = Icons.Default.Download,
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )

            MenuOption(
                title = "Configurações",
                description = "Notification parser e preferências",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun MenuOption(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
