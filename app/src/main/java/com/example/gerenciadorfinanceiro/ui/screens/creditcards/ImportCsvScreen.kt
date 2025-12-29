package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.csv.CsvFormat
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import com.example.gerenciadorfinanceiro.util.formatMonthYear
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportCsvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            val fileName = getFileName(context, it)
            viewModel.onFileSelected(it, fileName) {
                context.contentResolver.openInputStream(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.step) {
                            ImportStep.SELECT_FILE -> "Importar CSV"
                            ImportStep.PREVIEW -> "Pré-visualização"
                            ImportStep.SUCCESS -> "Importação Concluída"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState.step) {
                            ImportStep.PREVIEW -> viewModel.goBack()
                            else -> onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.step) {
                ImportStep.SELECT_FILE -> {
                    SelectFileContent(
                        uiState = uiState,
                        onSelectFile = {
                            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                        },
                        onFormatSelected = viewModel::setFormat,
                        onPreviousMonth = viewModel::selectPreviousMonth,
                        onNextMonth = viewModel::selectNextMonth,
                        onParseFile = {
                            selectedFileUri?.let { uri ->
                                viewModel.parseFile { context.contentResolver.openInputStream(uri) }
                            }
                        }
                    )
                }
                ImportStep.PREVIEW -> {
                    PreviewContent(
                        uiState = uiState,
                        onImport = { viewModel.importBill() },
                        onGoBack = viewModel::goBack,
                        onToggleItem = viewModel::toggleItemSelection,
                        onSelectAll = viewModel::selectAllItems,
                        onDeselectAll = viewModel::deselectAllItems
                    )
                }
                ImportStep.SUCCESS -> {
                    SuccessContent(
                        uiState = uiState,
                        onDone = onNavigateBack,
                        onImportAnother = viewModel::reset
                    )
                }
            }

            // Error Snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFileContent(
    uiState: ImportCsvUiState,
    onSelectFile: () -> Unit,
    onFormatSelected: (CsvFormat) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onParseFile: () -> Unit
) {
    var showFormatDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Info
        item {
            uiState.creditCard?.let { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${card.bank.displayName} •••• ${card.lastFourDigits}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Month Selector
        item {
            Text(
                text = "Mês da Fatura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
                    }
                    Text(
                        text = formatMonthYear(uiState.selectedMonth, uiState.selectedYear),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
                    }
                }
            }
        }

        // Format Selector
        item {
            Text(
                text = "Formato do CSV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = showFormatDropdown,
                onExpandedChange = { showFormatDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedFormat.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Formato") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFormatDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = showFormatDropdown,
                    onDismissRequest = { showFormatDropdown = false }
                ) {
                    CsvFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                onFormatSelected(format)
                                showFormatDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // File Selection
        item {
            Text(
                text = "Arquivo CSV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSelectFile
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (uiState.fileUri != null) Icons.AutoMirrored.Filled.InsertDriveFile else Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (uiState.fileUri != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.fileName ?: "Selecionar arquivo CSV",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (uiState.fileUri != null) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.fileUri == null) {
                            Text(
                                text = "Toque para selecionar um arquivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.fileUri != null) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Parse Button
        item {
            Button(
                onClick = onParseFile,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.fileUri != null && !uiState.isParsing
            ) {
                if (uiState.isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isParsing) "Analisando..." else "Analisar Arquivo")
            }
        }

        // Help Text
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Dicas para importação",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Exporte a fatura do seu banco em formato CSV\n" +
                               "• O arquivo deve conter: data, descrição e valor\n" +
                               "• Se usar formato genérico, use ponto-e-vírgula (;) como separador\n" +
                               "• Valores em reais serão convertidos automaticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    uiState: ImportCsvUiState,
    onImport: () -> Unit,
    onGoBack: () -> Unit,
    onToggleItem: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
    val allSelected = uiState.selectedItems.size == uiState.previewItems.size
    val noneSelected = uiState.selectedItems.isEmpty()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fatura ${formatMonthYear(uiState.selectedMonth, uiState.selectedYear)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.previewTotal.toReais(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "${uiState.selectedItems.size} de ${uiState.previewItems.size} itens selecionados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Items List Header with Select All/None
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Itens a importar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onSelectAll,
                    enabled = !allSelected
                ) {
                    Text("Todos")
                }
                TextButton(
                    onClick = onDeselectAll,
                    enabled = !noneSelected
                ) {
                    Text("Nenhum")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.previewItems.size) { index ->
                val item = uiState.previewItems[index]
                val isSelected = index in uiState.selectedItems
                PreviewItemCard(
                    item = item,
                    dateFormatter = dateFormatter,
                    isSelected = isSelected,
                    onToggle = { onToggleItem(index) }
                )
            }
        }

        // Bottom Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Voltar")
            }
            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isImporting && uiState.selectedItems.isNotEmpty()
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isImporting) "Importando..." else "Importar (${uiState.selectedItems.size})")
            }
        }
    }
}

@Composable
private fun PreviewItemCard(
    item: CsvBillItem,
    dateFormatter: DateTimeFormatter,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormatter.format(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) item.category.color else item.category.color.copy(alpha = 0.6f)
                    )
                    if (item.totalInstallments > 1) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.installmentNumber}/${item.totalInstallments}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = item.amount.toReais(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    uiState: ImportCsvUiState,
    onDone: () -> Unit,
    onImportAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Importação Concluída!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        uiState.importSuccess?.let { success ->
            Text(
                text = "${success.itemCount} itens importados",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: ${success.totalAmount.toReais()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Concluir")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onImportAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Importar Outro Arquivo")
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        if (nameIndex >= 0) cursor.getString(nameIndex) else null
    }
}

