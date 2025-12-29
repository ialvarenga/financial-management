package com.example.gerenciadorfinanceiro.ui.screens.recurrences

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toReais

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencesScreen(
    onNavigateToAddEdit: (Long?) -> Unit,
    viewModel: RecurrencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var recurrenceToDelete by remember { mutableStateOf<Recurrence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recorrências") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar recorrência")
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
        } else if (uiState.recurrences.isEmpty()) {
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
                        Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nenhuma recorrência cadastrada",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Adicione recorrências para pagamentos fixos",
                        style = MaterialTheme.typography.bodyMedium,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.recurrences, key = { it.id }) { recurrence ->
                    RecurrenceItem(
                        recurrence = recurrence,
                        onClick = { onNavigateToAddEdit(recurrence.id) },
                        onDelete = { recurrenceToDelete = recurrence },
                        onToggleActive = { viewModel.toggleActive(recurrence) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    recurrenceToDelete?.let { recurrence ->
        AlertDialog(
            onDismissRequest = { recurrenceToDelete = null },
            title = { Text("Excluir recorrência") },
            text = { Text("Deseja excluir a recorrência '${recurrence.description}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecurrence(recurrence)
                    recurrenceToDelete = null
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { recurrenceToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RecurrenceItem(
    recurrence: Recurrence,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (recurrence.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
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
                Icon(
                    imageVector = when (recurrence.frequency) {
                        Frequency.DAILY -> Icons.Default.Today
                        Frequency.WEEKLY -> Icons.Default.CalendarMonth
                        Frequency.MONTHLY -> Icons.Default.DateRange
                        Frequency.YEARLY -> Icons.Default.CalendarToday
                    },
                    contentDescription = null,
                    tint = if (recurrence.type == TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recurrence.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recurrence.frequency.toDisplayString()} • ${recurrence.category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = recurrence.amount.toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (recurrence.type == TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (recurrence.isActive) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = if (recurrence.isActive) "Ativa" else "Inativa",
                        tint = if (recurrence.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
    }
}

private fun Frequency.toDisplayString(): String = when (this) {
    Frequency.DAILY -> "Diário"
    Frequency.WEEKLY -> "Semanal"
    Frequency.MONTHLY -> "Mensal"
    Frequency.YEARLY -> "Anual"
}
