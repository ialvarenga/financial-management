package com.example.gerenciadorfinanceiro.ui.screens.mais

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaisScreen(
    onNavigateToCreditCards: () -> Unit,
    onNavigateToRecurrences: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToDeveloperTools: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mais") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuOption(
                title = "Cartões de Crédito",
                description = "Gerencie seus cartões e faturas",
                icon = Icons.Default.CreditCard,
                onClick = onNavigateToCreditCards
            )

            MenuOption(
                title = "Recorrências",
                description = "Transações recorrentes e agendamentos",
                icon = Icons.Default.Repeat,
                onClick = onNavigateToRecurrences
            )

            MenuOption(
                title = "Análises e Gráficos",
                description = "Visualize suas finanças em gráficos",
                icon = Icons.Default.Analytics,
                onClick = onNavigateToAnalytics
            )

            MenuOption(
                title = "Ferramentas de Dados",
                description = "Exportar, importar e resetar dados do aplicativo",
                icon = Icons.Default.Storage,
                onClick = onNavigateToDeveloperTools
            )

            MenuOption(
                title = "Configurações",
                description = "Notification parser e preferências",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun MenuOption(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
