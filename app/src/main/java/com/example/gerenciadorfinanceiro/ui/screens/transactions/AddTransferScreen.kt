package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransferViewModel = hiltViewModel()
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
                title = { Text(if (uiState.isEditing) "Editar Transferência" else "Nova Transferência") },
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
                singleLine = true,
                placeholder = { Text("Ex: Transferência entre contas") }
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
                placeholder = { Text("0,00") }
            )

            // From Account Dropdown
            var expandedFromAccount by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedFromAccount,
                onExpandedChange = { expandedFromAccount = it }
            ) {
                OutlinedTextField(
                    value = uiState.fromAccount?.let { "${it.name} (${it.bank.displayName})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conta de Origem") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFromAccount) },
                    leadingIcon = {
                        uiState.fromAccount?.bank?.iconResId?.let { iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = uiState.fromAccount?.bank?.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        } ?: Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.errorMessage?.contains("origem") == true
                )
                ExposedDropdownMenu(
                    expanded = expandedFromAccount,
                    onDismissRequest = { expandedFromAccount = false }
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
                                viewModel.onFromAccountChange(account)
                                expandedFromAccount = false
                            },
                            enabled = account.id != uiState.toAccount?.id
                        )
                    }
                }
            }

            // To Account Dropdown
            var expandedToAccount by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedToAccount,
                onExpandedChange = { expandedToAccount = it }
            ) {
                OutlinedTextField(
                    value = uiState.toAccount?.let { "${it.name} (${it.bank.displayName})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conta de Destino") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToAccount) },
                    leadingIcon = {
                        uiState.toAccount?.bank?.iconResId?.let { iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = uiState.toAccount?.bank?.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        } ?: Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.errorMessage?.contains("destino") == true
                )
                ExposedDropdownMenu(
                    expanded = expandedToAccount,
                    onDismissRequest = { expandedToAccount = false }
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
                                viewModel.onToAccountChange(account)
                                expandedToAccount = false
                            },
                            enabled = account.id != uiState.fromAccount?.id
                        )
                    }
                }
            }

            // Fee (optional)
            OutlinedTextField(
                value = uiState.fee,
                onValueChange = viewModel::onFeeChange,
                label = { Text("Taxa (R$) - Opcional") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                singleLine = true,
                placeholder = { Text("0,00") },
                supportingText = { Text("Taxa cobrada pela transferência (descontada da conta de origem)") }
            )

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

            // Date
            OutlinedTextField(
                value = uiState.date.format(dateFormatter),
                onValueChange = {},
                label = { Text("Data") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Observações (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

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
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isEditing) "Atualizar Transferência" else "Criar Transferência")
            }
        }
    }
}

