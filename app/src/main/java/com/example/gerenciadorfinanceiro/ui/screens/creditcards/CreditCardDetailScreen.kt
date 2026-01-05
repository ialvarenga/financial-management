package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToAddItem: (Long) -> Unit,
    onNavigateToEditItem: (Long, Long) -> Unit,
    onNavigateToImportCsv: () -> Unit,
    viewModel: CreditCardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CreditCardItem?>(null) }
    var expandedBillIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.card?.name ?: "Detalhes do Cartão") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToImportCsv) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar CSV")
                    }
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.currentBill?.let { bill ->
                FloatingActionButton(onClick = { onNavigateToAddItem(bill.id) }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar item")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Info Section
                item {
                    uiState.card?.let { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = card.bank.displayName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "•••• ${card.lastFourDigits}",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                Divider()

                                // Limit usage section
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Limite Disponível",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = uiState.availableLimit.toReais(),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (uiState.availableLimit > 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Limite Total",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = card.creditLimit.toReais(),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }

                                    // Progress bar showing used limit
                                    val usagePercentage = if (card.creditLimit > 0) {
                                        (uiState.usedLimit.toFloat() / card.creditLimit.toFloat()).coerceIn(0f, 1f)
                                    } else 0f

                                    LinearProgressIndicator(
                                        progress = { usagePercentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = when {
                                            usagePercentage > 0.9f -> MaterialTheme.colorScheme.error
                                            usagePercentage > 0.7f -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Text(
                                        text = "Utilizado: ${uiState.usedLimit.toReais()} (${(usagePercentage * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }

                                Divider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Fechamento",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Dia ${card.closingDay}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Vencimento",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Dia ${card.dueDay}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Bill Section
                item {
                    Text(
                        text = "Fatura Atual",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                item {
                    val currentBill = uiState.currentBill
                    if (currentBill != null) {
                        BillCard(bill = currentBill)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma fatura para o mês atual",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Current Bill Items
                if (uiState.currentBill != null) {
                    item {
                        Text(
                            text = "Itens da Fatura",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.currentBillItems.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum item na fatura",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.currentBillItems
                        ) { item ->
                            CreditCardItemCard(
                                item = item,
                                onEdit = { onNavigateToEditItem(item.creditCardBillId, item.id) },
                                onDelete = { itemToDelete = item }
                            )
                        }
                    }
                }

                // Bill History Section
                item {
                    Text(
                        text = "Histórico de Faturas",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (uiState.billHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma fatura encontrada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.billHistory) { bill ->
                        val isExpanded = expandedBillIds.contains(bill.id)
                        val billItems = uiState.billItems[bill.id] ?: emptyList()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BillCard(
                                bill = bill,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedBillIds = if (isExpanded) {
                                        expandedBillIds - bill.id
                                    } else {
                                        expandedBillIds + bill.id
                                    }
                                }
                            )

                            // Show items when expanded
                            if (isExpanded) {
                                if (billItems.isEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nenhum item nesta fatura",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    billItems.forEach { item ->
                                        Box(modifier = Modifier.padding(start = 16.dp)) {
                                            CreditCardItemCard(
                                                item = item,
                                                onEdit = { onNavigateToEditItem(item.creditCardBillId, item.id) },
                                                onDelete = { itemToDelete = item }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete card confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir cartão") },
            text = { Text("Deseja excluir este cartão? Todas as faturas associadas também serão excluídas. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard()
                    showDeleteDialog = false
                    onNavigateBack()
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

    // Delete item confirmation dialog
    itemToDelete?.let { item ->
        val isInstallment = item.installmentGroupId != null
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Excluir item") },
            text = {
                Text(
                    if (isInstallment) {
                        "Este item faz parte de uma compra parcelada. Deseja excluir todas as ${item.totalInstallments} parcelas?"
                    } else {
                        "Deseja excluir este item?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item)
                    itemToDelete = null
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BillCard(
    bill: CreditCardBill,
    isExpanded: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
    val monthYear = Instant.ofEpochMilli(bill.closingDate)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
        .replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthYear,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BillStatusChip(status = bill.status)
                    if (onClick != null) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Recolher" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bill.totalAmount.toReais(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (bill.totalAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vencimento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val dueDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val dueDate = Instant.ofEpochMilli(bill.dueDate)
                        .atZone(ZoneId.systemDefault())
                        .format(dueDateFormatter)
                    Text(
                        text = dueDate,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun BillStatusChip(status: BillStatus) {
    val (text, color) = when (status) {
        BillStatus.OPEN -> "Aberta" to MaterialTheme.colorScheme.primary
        BillStatus.CLOSED -> "Fechada" to MaterialTheme.colorScheme.tertiary
        BillStatus.PAID -> "Paga" to MaterialTheme.colorScheme.secondary
        BillStatus.OVERDUE -> "Vencida" to MaterialTheme.colorScheme.error
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
fun CreditCardItemCard(
    item: CreditCardItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val purchaseDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val purchaseDate = Instant.ofEpochMilli(item.purchaseDate)
        .atZone(ZoneId.systemDefault())
        .format(purchaseDateFormatter)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category icon with color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = item.category.color
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = purchaseDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.totalInstallments > 1) {
                        Text(
                            text = "Parcela ${item.installmentNumber}/${item.totalInstallments}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.amount.toReais(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Mais opções"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
