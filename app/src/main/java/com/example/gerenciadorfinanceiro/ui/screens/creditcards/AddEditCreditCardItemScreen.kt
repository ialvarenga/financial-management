package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCreditCardItemScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCreditCardItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Item" else "Adicionar Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bill info
            uiState.bill?.let { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Fatura: ${bill.month}/${bill.year}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Bill Selector (only in edit mode)
            if (uiState.isEditing && uiState.canChangeBill && uiState.availableBills.isNotEmpty()) {
                Text(
                    text = "Alterar Fatura",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                var expandedBill by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandedBill,
                    onExpandedChange = { expandedBill = it }
                ) {
                    val selectedBillId = uiState.selectedBillId ?: uiState.bill?.id
                    val selectedBill = uiState.availableBills.find { it.id == selectedBillId }
                        ?: uiState.bill

                    OutlinedTextField(
                        value = selectedBill?.let { "${it.month}/${it.year}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fatura") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBill)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        supportingText = {
                            if (uiState.editingItem?.installmentGroupId != null) {
                                Text(
                                    text = "Todas as parcelas serão movidas juntas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expandedBill,
                        onDismissRequest = { expandedBill = false }
                    ) {
                        uiState.availableBills.forEach { bill ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${bill.month}/${bill.year}")
                                        Text(
                                            text = bill.totalAmount.toReais(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onBillChange(bill.id)
                                    expandedBill = false
                                },
                                leadingIcon = if (bill.id == selectedBillId) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }

                // Warning card for installment moves
                if (uiState.editingItem?.installmentGroupId != null &&
                    uiState.selectedBillId != null &&
                    uiState.selectedBillId != uiState.bill?.id
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Atenção: Todas as ${uiState.installments} parcelas serão " +
                                        "movidas para as faturas correspondentes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("Descrição") == true,
                singleLine = true
            )

            // Amount
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("Valor") == true,
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                singleLine = true,
                placeholder = { Text("0,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Date with DatePicker
            val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
            val purchaseDate = remember(uiState.purchaseDate) {
                Instant.ofEpochMilli(uiState.purchaseDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            var showDatePicker by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = purchaseDate.format(dateFormatter),
                onValueChange = {},
                label = { Text("Data da compra") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (showDatePicker) {
                // Convert local date to UTC for DatePicker (which uses UTC internally)
                val initialDateUtc = remember(uiState.purchaseDate) {
                    val localDate = Instant.ofEpochMilli(uiState.purchaseDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    localDate.atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = initialDateUtc
                    )
                    DatePicker(
                        state = datePickerState,
                        title = { Text("Selecione a data", modifier = Modifier.padding(16.dp)) }
                    )

                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // DatePicker returns UTC midnight, convert to local midnight
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val localMillis = localDate.atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            viewModel.onPurchaseDateChange(localMillis)
                        }
                    }
                }
            }

            // Category Dropdown
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    uiState.availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Installments Selector
            // Check original item's installments, not current UI state (which changes when converting)
            val canEditInstallments = !uiState.isEditing || (uiState.editingItem?.totalInstallments ?: 1) == 1

            if (canEditInstallments) {
                // For editing single items, show option to convert to installment
                if (uiState.isEditing && uiState.editingItem?.totalInstallments == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Este item é uma parcela",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            checked = uiState.isConvertingToInstallment,
                            onCheckedChange = viewModel::onConvertingToInstallmentChange
                        )
                    }

                    if (uiState.isConvertingToInstallment) {
                        // Show current installment number selector
                        var expandedCurrentInstallment by remember { mutableStateOf(false) }
                        var expandedTotalInstallments by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Current installment number
                            ExposedDropdownMenuBox(
                                expanded = expandedCurrentInstallment,
                                onExpandedChange = { expandedCurrentInstallment = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = "Parcela ${uiState.currentInstallmentNumber}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Esta é a") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrentInstallment) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCurrentInstallment,
                                    onDismissRequest = { expandedCurrentInstallment = false }
                                ) {
                                    (1..uiState.installments).forEach { num ->
                                        DropdownMenuItem(
                                            text = { Text("Parcela $num") },
                                            onClick = {
                                                viewModel.onCurrentInstallmentNumberChange(num)
                                                expandedCurrentInstallment = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Total installments
                            ExposedDropdownMenuBox(
                                expanded = expandedTotalInstallments,
                                onExpandedChange = { expandedTotalInstallments = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = "de ${uiState.installments}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Total") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTotalInstallments) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedTotalInstallments,
                                    onDismissRequest = { expandedTotalInstallments = false }
                                ) {
                                    (2..24).forEach { total ->
                                        DropdownMenuItem(
                                            text = { Text("de $total") },
                                            onClick = {
                                                viewModel.onInstallmentsChange(total)
                                                // Adjust current installment if needed
                                                if (uiState.currentInstallmentNumber > total) {
                                                    viewModel.onCurrentInstallmentNumberChange(total)
                                                }
                                                expandedTotalInstallments = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Info card showing what will happen
                        val remainingInstallments = uiState.installments - uiState.currentInstallmentNumber
                        if (remainingInstallments > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Serão criadas $remainingInstallments parcelas adicionais nas próximas faturas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // For new items: standard installment selector
                    Text("Parcelas", style = MaterialTheme.typography.titleSmall)
                    var expandedInstallments by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedInstallments,
                        onExpandedChange = { expandedInstallments = it }
                    ) {
                        OutlinedTextField(
                            value = if (uiState.installments == 1) {
                                "À vista"
                            } else {
                                "${uiState.installments}x"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Número de parcelas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInstallments) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedInstallments,
                            onDismissRequest = { expandedInstallments = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("À vista") },
                                onClick = {
                                    viewModel.onInstallmentsChange(1)
                                    expandedInstallments = false
                                }
                            )
                            (2..12).forEach { installments ->
                                DropdownMenuItem(
                                    text = { Text("${installments}x") },
                                    onClick = {
                                        viewModel.onInstallmentsChange(installments)
                                        expandedInstallments = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Show read-only installment info for items in installment groups
                OutlinedTextField(
                    value = "Parcela ${uiState.editingItem?.installmentNumber}/${uiState.installments}",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Parcelas (não editável)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Este item faz parte de uma compra parcelada. Para alterar, exclua todas as parcelas e crie novamente.")
                    }
                )
            }

            if (uiState.installments > 1 && !uiState.isEditing && !uiState.isConvertingToInstallment) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Esta compra será dividida em ${uiState.installments} parcelas nas próximas faturas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.isEditing) "Salvar" else "Adicionar")
                }
            }
        }
    }
}
