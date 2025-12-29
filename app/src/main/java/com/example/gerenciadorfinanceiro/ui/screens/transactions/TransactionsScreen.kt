package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.formatMonthYear
import com.example.gerenciadorfinanceiro.util.toLocalDate
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToAddEdit: (Long?) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var transactionToDelete by remember { mutableStateOf<TransactionWithAccount?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações") },
                actions = {
                    IconButton(onClick = { showMonthPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar mês")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar transação")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = formatMonthYear(uiState.selectedMonth, uiState.selectedYear),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Receitas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.totalIncome.toReais(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Despesas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.totalExpense.toReais(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Column {
                            Text(
                                text = "Saldo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (uiState.totalIncome - uiState.totalExpense).toReais(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterType == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = uiState.filterType == TransactionType.INCOME,
                    onClick = { viewModel.setFilter(TransactionType.INCOME) },
                    label = { Text("Receitas") }
                )
                FilterChip(
                    selected = uiState.filterType == TransactionType.EXPENSE,
                    onClick = { viewModel.setFilter(TransactionType.EXPENSE) },
                    label = { Text("Despesas") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transactions and projected recurrences list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredTransactions.isEmpty() && uiState.projectedRecurrences.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma transação ou recorrência encontrada")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Transactions section
                    items(uiState.filteredTransactions, key = { "transaction_${it.transaction.id}" }) { transactionWithAccount ->
                        TransactionItem(
                            transactionWithAccount = transactionWithAccount,
                            onClick = { onNavigateToAddEdit(transactionWithAccount.transaction.id) },
                            onDelete = { transactionToDelete = transactionWithAccount },
                            onComplete = { viewModel.completeTransaction(transactionWithAccount.transaction.id) }
                        )
                    }

                    // Projected recurrences section
                    if (uiState.projectedRecurrences.isNotEmpty()) {
                        item(key = "recurrences_header") {
                            Text(
                                text = "Recorrências Projetadas",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(uiState.projectedRecurrences, key = { "recurrence_${it.recurrence.id}_${it.projectedDate}" }) { projectedRecurrence ->
                            ProjectedRecurrenceItem(
                                projectedRecurrence = projectedRecurrence,
                                accounts = uiState.transactions.map { it.account }.distinctBy { it.id },
                                onConfirm = { accountId ->
                                    viewModel.confirmRecurrence(projectedRecurrence, accountId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    transactionToDelete?.let { transactionWithAccount ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Excluir transação") },
            text = { Text("Deseja excluir a transação '${transactionWithAccount.transaction.description}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transactionWithAccount.transaction.id)
                    transactionToDelete = null
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TransactionItem(
    transactionWithAccount: TransactionWithAccount,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    val transaction = transactionWithAccount.transaction
    val account = transactionWithAccount.account
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${account.name} • ${transaction.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.date.toLocalDate().format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.status == TransactionStatus.PENDING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = onComplete,
                        label = { Text("Concluir", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transaction.amount.toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (transaction.status == TransactionStatus.COMPLETED) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Concluída",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Pendente",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectedRecurrenceItem(
    projectedRecurrence: ProjectedRecurrence,
    accounts: List<com.example.gerenciadorfinanceiro.data.local.entity.Account>,
    onConfirm: (Long?) -> Unit
) {
    val recurrence = projectedRecurrence.recurrence
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val projectedDate = Instant.ofEpochMilli(projectedRecurrence.projectedDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    var showAccountSelector by remember { mutableStateOf(false) }
    val needsAccountSelection = recurrence.accountId == null && recurrence.creditCardId == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Recorrência",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = recurrence.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recurrence.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = projectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = {
                        if (needsAccountSelection) {
                            showAccountSelector = true
                        } else {
                            onConfirm(null)
                        }
                    },
                    label = { Text("Confirmar", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = recurrence.amount.toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (recurrence.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }

    // Account selector dialog for unassigned recurrences
    if (showAccountSelector) {
        AlertDialog(
            onDismissRequest = { showAccountSelector = false },
            title = { Text("Selecionar conta") },
            text = {
                Column {
                    Text(
                        text = "Escolha a conta para esta transação:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    accounts.forEach { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onConfirm(account.id)
                                    showAccountSelector = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = account.bank.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = account.balance.toReais(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAccountSelector = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
