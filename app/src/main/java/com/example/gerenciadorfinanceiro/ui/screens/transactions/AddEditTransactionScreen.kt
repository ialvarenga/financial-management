package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Transação" else "Nova Transação") },
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
            // Transaction Type Toggle
            Text("Tipo", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Despesa") },
                    leadingIcon = if (uiState.type == TransactionType.EXPENSE) {
                        { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                    } else null
                )
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    label = { Text("Receita") },
                    leadingIcon = if (uiState.type == TransactionType.INCOME) {
                        { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                    } else null
                )
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

            // Account Dropdown
            var expandedAccount by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedAccount,
                onExpandedChange = { expandedAccount = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conta") },
                    leadingIcon = {
                        uiState.selectedAccount?.bank?.iconResId?.let { iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = uiState.selectedAccount?.bank?.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.errorMessage?.contains("conta") == true
                )
                ExposedDropdownMenu(
                    expanded = expandedAccount,
                    onDismissRequest = { expandedAccount = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (${account.bank.displayName})") },
                            leadingIcon = {
                                account.bank.iconResId?.let { iconRes ->
                                    Image(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = account.bank.displayName,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.onAccountChange(account)
                                expandedAccount = false
                            }
                        )
                    }
                }
            }

            // Category Dropdown
            var expandedCategory by remember { mutableStateOf(false) }
            val availableCategories = if (uiState.type == TransactionType.INCOME) {
                Category.incomes()
            } else {
                Category.expenses()
            }

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
                    availableCategories.forEach { category ->
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

            // Payment Method Dropdown
            var expandedPaymentMethod by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedPaymentMethod,
                onExpandedChange = { expandedPaymentMethod = it }
            ) {
                OutlinedTextField(
                    value = uiState.paymentMethod.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Forma de Pagamento") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaymentMethod) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedPaymentMethod,
                    onDismissRequest = { expandedPaymentMethod = false }
                ) {
                    PaymentMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.displayName) },
                            onClick = {
                                viewModel.onPaymentMethodChange(method)
                                expandedPaymentMethod = false
                            }
                        )
                    }
                }
            }

            // Status Toggle
            Text("Status", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.status == TransactionStatus.PENDING,
                    onClick = { viewModel.onStatusChange(TransactionStatus.PENDING) },
                    label = { Text("Pendente") }
                )
                FilterChip(
                    selected = uiState.status == TransactionStatus.COMPLETED,
                    onClick = { viewModel.onStatusChange(TransactionStatus.COMPLETED) },
                    label = { Text("Concluída") }
                )
            }

            // Date with DatePicker
            var showDatePicker by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.date.format(dateFormatter),
                onValueChange = {},
                label = { Text("Data") },
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
                        initialSelectedDateMillis = uiState.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    DatePicker(
                        state = datePickerState,
                        title = { Text("Selecione a data", modifier = Modifier.padding(16.dp)) }
                    )

                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.onDateChange(selectedDate)
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Observações (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Recurrence dropdown - to associate transaction with a recurrence
            var expandedRecurrence by remember { mutableStateOf(false) }
            val filteredRecurrences = uiState.recurrences.filter { it.type == uiState.type }

            ExposedDropdownMenuBox(
                expanded = expandedRecurrence,
                onExpandedChange = { expandedRecurrence = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedRecurrence?.description ?: "Nenhuma",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Recorrência (opcional)") },
                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecurrence) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedRecurrence,
                    onDismissRequest = { expandedRecurrence = false }
                ) {
                    // Option to remove recurrence association
                    DropdownMenuItem(
                        text = { Text("Nenhuma") },
                        onClick = {
                            viewModel.onRecurrenceChange(null)
                            expandedRecurrence = false
                        }
                    )
                    // Show filtered recurrences (same type as the transaction)
                    filteredRecurrences.forEach { recurrence ->
                        DropdownMenuItem(
                            text = { Text(recurrence.description) },
                            onClick = {
                                viewModel.onRecurrenceChange(recurrence)
                                expandedRecurrence = false
                            }
                        )
                    }
                }
            }

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
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
                    Text(if (uiState.isEditing) "Salvar" else "Criar")
                }
            }
        }
    }
}
