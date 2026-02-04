package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val activeAccounts by viewModel.activeAccounts.collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CreditCardItem?>(null) }
    var expandedBillIds by remember { mutableStateOf(setOf<Long>()) }
    var isCurrentBillExpanded by remember { mutableStateOf(false) }
    var showMarkAsPaidDialog by remember { mutableStateOf(false) }
    var billToMarkAsPaid by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedBillId by remember { mutableStateOf<Long?>(null) }
    var showCloseBillDialog by remember { mutableStateOf(false) }

    // Find the selected bill to check its status
    val selectedBill = selectedBillId?.let { id ->
        if (uiState.currentBill?.id == id) uiState.currentBill
        else uiState.billHistory.find { it.id == id }
    }

    // Back handler to clear selection
    BackHandler(enabled = selectedBillId != null) {
        selectedBillId = null
    }

    Scaffold(
        topBar = {
            if (selectedBillId != null) {
                TopAppBar(
                    title = { Text("Fatura selecionada") },
                    navigationIcon = {
                        IconButton(onClick = { selectedBillId = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                        }
                    },
                    actions = {
                        if (selectedBill?.status == BillStatus.OPEN) {
                            IconButton(onClick = { showCloseBillDialog = true }) {
                                Icon(Icons.Default.Lock, contentDescription = "Fechar fatura")
                            }
                        }
                        if (selectedBill?.status == BillStatus.CLOSED) {
                            IconButton(onClick = {
                                billToMarkAsPaid = selectedBillId
                                showMarkAsPaidDialog = true
                                selectedBillId = null
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Marcar como paga")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
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
            }
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BillCard(
                                bill = currentBill,
                                isExpanded = isCurrentBillExpanded,
                                isSelected = selectedBillId == currentBill.id,
                                onClick = {
                                    if (selectedBillId != null) {
                                        selectedBillId = null
                                    } else {
                                        isCurrentBillExpanded = !isCurrentBillExpanded
                                    }
                                },
                                onLongClick = {
                                    selectedBillId = currentBill.id
                                },
                                onMarkAsPaid = {
                                    billToMarkAsPaid = currentBill.id
                                    showMarkAsPaidDialog = true
                                }
                            )

                            // Show items when expanded
                            if (isCurrentBillExpanded) {
                                if (uiState.currentBillItems.isEmpty()) {
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
                                    val groupedItems = groupItemsByDate(uiState.currentBillItems)
                                    groupedItems.forEach { (date, items) ->
                                        Box(modifier = Modifier.padding(start = 16.dp)) {
                                            DateHeader(date = date)
                                        }
                                        items.forEach { item ->
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

                // Bill History Section Header with Sort Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico de Faturas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = {
                                viewModel.setSortOrder(
                                    if (uiState.sortOrder == BillSortOrder.NEWEST_FIRST)
                                        BillSortOrder.OLDEST_FIRST
                                    else
                                        BillSortOrder.NEWEST_FIRST
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.sortOrder == BillSortOrder.NEWEST_FIRST)
                                    Icons.Default.ArrowDownward
                                else
                                    Icons.Default.ArrowUpward,
                                contentDescription = if (uiState.sortOrder == BillSortOrder.NEWEST_FIRST)
                                    "Ordenar mais antigas primeiro"
                                else
                                    "Ordenar mais recentes primeiro"
                            )
                        }
                    }
                }

                // Status Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillStatusFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = uiState.statusFilter == filter,
                                onClick = { viewModel.setStatusFilter(filter) },
                                label = { Text(filter.displayName) }
                            )
                        }
                    }
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
                                isSelected = selectedBillId == bill.id,
                                onClick = {
                                    if (selectedBillId != null) {
                                        selectedBillId = null
                                    } else {
                                        expandedBillIds = if (isExpanded) {
                                            expandedBillIds - bill.id
                                        } else {
                                            expandedBillIds + bill.id
                                        }
                                    }
                                },
                                onLongClick = {
                                    selectedBillId = bill.id
                                },
                                onMarkAsPaid = {
                                    billToMarkAsPaid = bill.id
                                    showMarkAsPaidDialog = true
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
                                    val groupedBillItems = groupItemsByDate(billItems)
                                    groupedBillItems.forEach { (date, items) ->
                                        Box(modifier = Modifier.padding(start = 16.dp)) {
                                            DateHeader(date = date)
                                        }
                                        items.forEach { item ->
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

    // Close bill confirmation dialog
    if (showCloseBillDialog && selectedBillId != null) {
        AlertDialog(
            onDismissRequest = {
                showCloseBillDialog = false
            },
            title = { Text("Fechar fatura") },
            text = { Text("Deseja fechar esta fatura? Após fechada, ela ficará aguardando pagamento.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBillId?.let { viewModel.closeBill(it) }
                    showCloseBillDialog = false
                    selectedBillId = null
                }) {
                    Text("Fechar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCloseBillDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mark as paid confirmation dialog with account selection
    if (showMarkAsPaidDialog) {
        var expandedAccountDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showMarkAsPaidDialog = false
                billToMarkAsPaid = null
                selectedAccountId = null
            },
            title = { Text("Marcar fatura como paga") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Selecione a conta que será usada para pagar esta fatura:")

                    // Account selector dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedAccountDropdown,
                        onExpandedChange = { expandedAccountDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = activeAccounts.find { it.id == selectedAccountId }?.name ?: "Selecione uma conta",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Conta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedAccountDropdown,
                            onDismissRequest = { expandedAccountDropdown = false }
                        ) {
                            activeAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(account.name)
                                            Text(
                                                text = "${account.bank.displayName} - ${account.balance.toReais()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedAccountId = account.id
                                        expandedAccountDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (activeAccounts.isEmpty()) {
                        Text(
                            text = "Nenhuma conta ativa encontrada. Crie uma conta primeiro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        billToMarkAsPaid?.let { billId ->
                            selectedAccountId?.let { accountId ->
                                viewModel.markBillAsPaid(billId, accountId)
                                showMarkAsPaidDialog = false
                                billToMarkAsPaid = null
                                selectedAccountId = null
                            }
                        }
                    },
                    enabled = selectedAccountId != null
                ) {
                    Text("Marcar como Paga", color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMarkAsPaidDialog = false
                    billToMarkAsPaid = null
                    selectedAccountId = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillCard(
    bill: CreditCardBill,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMarkAsPaid: (() -> Unit)? = null
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
    val monthYear = Instant.ofEpochMilli(bill.closingDate)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
        .replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
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

            // Show "Mark as Paid" button for closed unpaid bills
            if (bill.status == BillStatus.CLOSED && onMarkAsPaid != null) {
                Button(
                    onClick = onMarkAsPaid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marcar como Paga")
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

@Composable
fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val displayText = when (date) {
        today -> "Hoje"
        yesterday -> "Ontem"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
            date.format(formatter).replaceFirstChar { it.uppercase() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Groups credit card items by their purchase date
 */
fun groupItemsByDate(items: List<CreditCardItem>): Map<LocalDate, List<CreditCardItem>> {
    return items.groupBy { item ->
        Instant.ofEpochMilli(item.purchaseDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.toSortedMap(compareByDescending { it })
}
