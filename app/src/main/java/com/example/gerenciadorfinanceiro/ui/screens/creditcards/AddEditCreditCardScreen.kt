package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Bank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCreditCardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCreditCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBankDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Cartão" else "Novo Cartão") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome do cartão") },
                placeholder = { Text("Ex: Cartão Principal") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null,
                singleLine = true
            )

            // Last Four Digits
            OutlinedTextField(
                value = uiState.lastFourDigits,
                onValueChange = viewModel::onLastFourDigitsChange,
                label = { Text("Últimos 4 dígitos") },
                placeholder = { Text("1234") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Credit Limit
            OutlinedTextField(
                value = uiState.creditLimit,
                onValueChange = viewModel::onCreditLimitChange,
                label = { Text("Limite do cartão (R$)") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Bank Dropdown
            ExposedDropdownMenuBox(
                expanded = showBankDropdown,
                onExpandedChange = { showBankDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.bank.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Banco") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBankDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showBankDropdown,
                    onDismissRequest = { showBankDropdown = false }
                ) {
                    Bank.values().forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.displayName) },
                            onClick = {
                                viewModel.onBankChange(bank)
                                showBankDropdown = false
                            }
                        )
                    }
                }
            }

            // Closing Day
            OutlinedTextField(
                value = uiState.closingDay,
                onValueChange = viewModel::onClosingDayChange,
                label = { Text("Dia de fechamento") },
                placeholder = { Text("1-31") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Dia do mês em que a fatura fecha") },
                singleLine = true
            )

            // Due Day
            OutlinedTextField(
                value = uiState.dueDay,
                onValueChange = viewModel::onDueDayChange,
                label = { Text("Dia de vencimento") },
                placeholder = { Text("1-31") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Dia do mês em que a fatura vence") },
                singleLine = true
            )

            // Payment Account Dropdown (optional)
            ExposedDropdownMenuBox(
                expanded = showAccountDropdown,
                onExpandedChange = { showAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.accounts.find { it.id == uiState.paymentAccountId }?.name ?: "Nenhuma",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conta para pagamento (opcional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma") },
                        onClick = {
                            viewModel.onPaymentAccountChange(null)
                            showAccountDropdown = false
                        }
                    )
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onPaymentAccountChange(account.id)
                                showAccountDropdown = false
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

            Spacer(modifier = Modifier.height(8.dp))

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
                    Text(if (uiState.isEditing) "Salvar" else "Criar")
                }
            }
        }
    }
}
