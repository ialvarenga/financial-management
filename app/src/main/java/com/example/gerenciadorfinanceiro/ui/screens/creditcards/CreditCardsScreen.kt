package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.util.toReais

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    onNavigateToAddEdit: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CreditCardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cartões de Crédito") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar cartão")
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
        } else if (uiState.cardsWithBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Nenhum cartão cadastrado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cardsWithBills, key = { it.card.id }) { cardWithBill ->
                    CreditCardItem(
                        card = cardWithBill.card,
                        currentBillAmount = cardWithBill.currentBillAmount,
                        onClick = { onNavigateToDetail(cardWithBill.card.id) },
                        onEdit = { onNavigateToAddEdit(cardWithBill.card.id) },
                        onDelete = { cardToDelete = cardWithBill.card }
                    )
                }

                if (uiState.cardsWithBills.isNotEmpty()) {
                    item {
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Fatura Atual",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = uiState.totalCurrentBills.toReais(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Excluir cartão") },
            text = { Text("Deseja excluir o cartão '${card.name}'? Todas as faturas associadas também serão excluídas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(card)
                    cardToDelete = null
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CreditCardItem(
    card: CreditCard,
    currentBillAmount: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Display bank icon if available, otherwise show generic credit card icon
                    card.bank.iconResId?.let { iconRes ->
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = card.bank.displayName,
                            modifier = Modifier.size(40.dp)
                        )
                    } ?: Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${card.bank.displayName} •••• ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Limite",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = card.creditLimit.toReais(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Fatura Atual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentBillAmount.toReais(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (currentBillAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Fech./Venc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Dia ${card.closingDay} / ${card.dueDay}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
