package com.example.gerenciadorfinanceiro.ui.screens.recurrences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurrenceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurrenceViewModel = hiltViewModel()
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
                title = { Text(if (uiState.isEditing) "Editar Recorrência" else "Nova Recorrência") },
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
                label = { Text("Valor") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("Valor") == true,
                prefix = { Text("R$ ") },
                singleLine = true
            )

            // Type (Income/Expense)
            Text("Tipo", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    label = { Text("Receita") }
                )
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Despesa") }
                )
            }

            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    val categories = if (uiState.type == TransactionType.INCOME) {
                        Category.incomes()
                    } else {
                        Category.expenses()
                    }
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Frequency
            Text("Frequência", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Frequency.values().forEach { frequency ->
                    FilterChip(
                        selected = uiState.frequency == frequency,
                        onClick = { viewModel.onFrequencyChange(frequency) },
                        label = {
                            Text(
                                when (frequency) {
                                    Frequency.DAILY -> "Diário"
                                    Frequency.WEEKLY -> "Semanal"
                                    Frequency.MONTHLY -> "Mensal"
                                    Frequency.YEARLY -> "Anual"
                                }
                            )
                        }
                    )
                }
            }

            // Day of month/week based on frequency
            when (uiState.frequency) {
                Frequency.MONTHLY, Frequency.YEARLY -> {
                    var dayExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Dia ${uiState.dayOfMonth}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dia do mês") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            (1..31).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text("Dia $day") },
                                    onClick = {
                                        viewModel.onDayOfMonthChange(day)
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Frequency.WEEKLY -> {
                    var dayExpanded by remember { mutableStateOf(false) }
                    val daysOfWeek = listOf(
                        1 to "Segunda-feira",
                        2 to "Terça-feira",
                        3 to "Quarta-feira",
                        4 to "Quinta-feira",
                        5 to "Sexta-feira",
                        6 to "Sábado",
                        7 to "Domingo"
                    )
                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = daysOfWeek.find { it.first == uiState.dayOfWeek }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dia da semana") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            daysOfWeek.forEach { (value, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.onDayOfWeekChange(value)
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Frequency.DAILY -> {
                    // No day selection needed for daily
                }
            }

            // Payment target (None, Account or Credit Card)
            Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)

            // Payment method dropdown - shows all methods
            var paymentMethodExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = paymentMethodExpanded,
                onExpandedChange = { paymentMethodExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.paymentMethod.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Método de Pagamento") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = paymentMethodExpanded,
                    onDismissRequest = { paymentMethodExpanded = false }
                ) {
                    PaymentMethod.allMethods().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.displayName) },
                            onClick = {
                                viewModel.onPaymentMethodChange(method)
                                paymentMethodExpanded = false
                            }
                        )
                    }
                }
            }

            // Account dropdown - only for DEBIT, PIX, TRANSFER
            if (uiState.paymentMethod.requiresAccount()) {
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedAccount?.name ?: "Selecione uma conta",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Conta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = uiState.errorMessage?.contains("conta") == true
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        uiState.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.bank.displayName})") },
                                onClick = {
                                    viewModel.onAccountChange(account)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Credit card dropdown - only for CREDIT_CARD
            if (uiState.paymentMethod == PaymentMethod.CREDIT_CARD) {
                var creditCardExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = creditCardExpanded,
                    onExpandedChange = { creditCardExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedCreditCard?.let { "${it.name} (${it.bank.displayName})" } ?: "Selecione um cartão",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cartão de Crédito") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = creditCardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = uiState.errorMessage?.contains("cartão") == true
                    )
                    ExposedDropdownMenu(
                        expanded = creditCardExpanded,
                        onDismissRequest = { creditCardExpanded = false }
                    ) {
                        uiState.creditCards.forEach { creditCard ->
                            DropdownMenuItem(
                                text = { Text("${creditCard.name} (${creditCard.bank.displayName})") },
                                onClick = {
                                    viewModel.onCreditCardChange(creditCard)
                                    creditCardExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Info text for BOLETO
            if (uiState.paymentMethod == PaymentMethod.BOLETO) {
                Text(
                    text = "A conta será escolhida no momento do pagamento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start date
            OutlinedTextField(
                value = uiState.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))),
                onValueChange = {},
                readOnly = true,
                label = { Text("Data de início") },
                modifier = Modifier.fillMaxWidth()
            )

            // Has end date checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = uiState.hasEndDate,
                    onCheckedChange = viewModel::onHasEndDateChange
                )
                Text(
                    "Tem data de término",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // End date (if has end date)
            if (uiState.hasEndDate) {
                OutlinedTextField(
                    value = uiState.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data de término") },
                    modifier = Modifier.fillMaxWidth()
                )
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

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
