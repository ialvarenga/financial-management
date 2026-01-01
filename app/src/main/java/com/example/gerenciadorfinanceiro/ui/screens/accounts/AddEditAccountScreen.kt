package com.example.gerenciadorfinanceiro.ui.screens.accounts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Bank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Conta" else "Nova Conta") },
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
            // Name field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome da conta") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage == "Nome é obrigatório",
                supportingText = if (uiState.errorMessage == "Nome é obrigatório") {
                    { Text(uiState.errorMessage!!) }
                } else null,
                singleLine = true
            )

            // Bank selector
            var bankExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = bankExpanded,
                onExpandedChange = { bankExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.bank.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Banco") },
                    leadingIcon = {
                        uiState.bank.iconResId?.let { iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = uiState.bank.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = bankExpanded,
                    onDismissRequest = { bankExpanded = false }
                ) {
                    Bank.entries.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.displayName) },
                            leadingIcon = {
                                bank.iconResId?.let { iconRes ->
                                    Image(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = bank.displayName,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.onBankChange(bank)
                                bankExpanded = false
                            }
                        )
                    }
                }
            }

            // Agency field
            OutlinedTextField(
                value = uiState.agency,
                onValueChange = viewModel::onAgencyChange,
                label = { Text("Agência") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage == "Agência é obrigatória",
                supportingText = if (uiState.errorMessage == "Agência é obrigatória") {
                    { Text(uiState.errorMessage!!) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Account number field
            OutlinedTextField(
                value = uiState.number,
                onValueChange = viewModel::onNumberChange,
                label = { Text("Número da conta") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage == "Número da conta é obrigatório",
                supportingText = if (uiState.errorMessage == "Número da conta é obrigatório") {
                    { Text(uiState.errorMessage!!) }
                } else null,
                singleLine = true
            )

            // Balance field
            OutlinedTextField(
                value = uiState.balance,
                onValueChange = viewModel::onBalanceChange,
                label = { Text("Saldo inicial") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage == "Saldo inválido",
                supportingText = if (uiState.errorMessage == "Saldo inválido") {
                    { Text(uiState.errorMessage!!) }
                } else {
                    { Text("Use vírgula para separar os centavos (ex: 1000,50)") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("R$ ") }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Delete button (only show when editing)
            if (uiState.isEditing) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir Conta")
                }
            }

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

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir conta") },
            text = { Text("Deseja excluir a conta '${uiState.name}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
