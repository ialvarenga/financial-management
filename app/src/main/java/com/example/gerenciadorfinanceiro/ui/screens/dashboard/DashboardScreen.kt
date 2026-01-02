package com.example.gerenciadorfinanceiro.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.formatMonthYear
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToRecurrences: () -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCreditCards: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = getGreeting(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getFormattedDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
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

                // Month Selector
                item {
                    MonthSelector(
                        month = uiState.selectedMonth,
                        year = uiState.selectedYear,
                        onPreviousMonth = viewModel::selectPreviousMonth,
                        onNextMonth = viewModel::selectNextMonth
                    )
                }

                // 5 Main Dashboard Boxes
                item {
                    DashboardBoxes(
                        dashboardData = uiState.dashboardData
                    )
                }

                // Quick Actions Section
                item {
                    QuickActionsSection(
                        onAddTransaction = onNavigateToAddTransaction,
                        onViewAccounts = onNavigateToAccounts,
                        onViewCreditCards = onNavigateToCreditCards,
                        onViewRecurrences = onNavigateToRecurrences
                    )
                }

                // Monthly Overview Card
                item {
                    MonthlyOverviewCard(
                        income = uiState.summary.monthlyIncome,
                        expenses = uiState.summary.monthlyExpenses,
                        balance = uiState.summary.monthlyBalance
                    )
                }

                // Accounts Section
                if (uiState.accounts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Suas Contas",
                            icon = Icons.Outlined.AccountBalance
                        )
                    }
                    item {
                        AccountsCarousel(accounts = uiState.accounts)
                    }
                }

                // Upcoming Bills Section
                if (uiState.projection.upcomingBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Faturas Pendentes",
                            icon = Icons.Outlined.CreditCard
                        )
                    }
                    items(
                        uiState.projection.upcomingBills.take(3),
                        key = { "bill_${it.id}" }
                    ) { bill ->
                        UpcomingBillCard(bill = bill)
                    }
                }

                // Upcoming Recurrences Section
                if (uiState.projectedRecurrences.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = "Próximas Recorrências",
                                icon = Icons.Outlined.Repeat,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onNavigateToRecurrences) {
                                Text("Ver Todas")
                            }
                        }
                    }
                    items(
                        uiState.projectedRecurrences
                            .sortedBy { it.projectedDate }
                            .take(5),
                        key = { "recurrence_${it.recurrence.id}_${it.projectedDate}" }
                    ) { projectedRecurrence ->
                        RecurrenceCard(projectedRecurrence = projectedRecurrence)
                    }
                }

                // Tips Section (Finance Bro Tips!)
                item {
                    FinanceTipCard(
                        currentBalance = uiState.dashboardData.currentBalance,
                        projectedExpenses = uiState.dashboardData.projectedExpenses,
                        fixedExpenses = uiState.dashboardData.fixedExpenses
                    )
                }

                // Empty State
                if (uiState.accounts.isEmpty() &&
                    uiState.projection.upcomingBills.isEmpty() &&
                    uiState.projectedRecurrences.isEmpty() &&
                    uiState.summary.totalBalance == 0L) {
                    item {
                        EmptyDashboardState()
                    }
                }

                // Bottom spacing for FAB or navigation bar
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Helper functions for greeting
@Composable
private fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Bom dia! ☀️"
        hour < 18 -> "Boa tarde! 🌤️"
        else -> "Boa noite! 🌙"
    }
}

@Composable
private fun getFormattedDate(): String {
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }
    val day = today.dayOfMonth
    val month = today.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    return "$dayOfWeek, $day de $month"
}


private enum class HealthStatus(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    EXCELLENT(
        "Você está tranquilo! 🎉",
        Icons.Filled.CheckCircle,
        Color(0xFF4CAF50)
    ),
    GOOD(
        "Situação controlada 👍",
        Icons.Filled.ThumbUp,
        Color(0xFF8BC34A)
    ),
    WARNING(
        "Orçamento apertado ⚠️",
        Icons.Filled.Warning,
        Color(0xFFFF9800)
    ),
    CRITICAL(
        "Saldo insuficiente! 🚨",
        Icons.Filled.Error,
        Color(0xFFF44336)
    )
}

// Quick Actions Section
@Composable
fun QuickActionsSection(
    onAddTransaction: () -> Unit,
    onViewAccounts: () -> Unit,
    onViewCreditCards: () -> Unit,
    onViewRecurrences: () -> Unit
) {
    Column {
        SectionHeader(
            title = "Ações Rápidas",
            icon = Icons.Outlined.FlashOn
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Outlined.Add,
                label = "Nova\nTransação",
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            QuickActionButton(
                icon = Icons.Outlined.AccountBalance,
                label = "Minhas\nContas",
                onClick = onViewAccounts,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            QuickActionButton(
                icon = Icons.Outlined.CreditCard,
                label = "Cartões\nde Crédito",
                onClick = onViewCreditCards,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            QuickActionButton(
                icon = Icons.Outlined.Repeat,
                label = "Recor-\nrências",
                onClick = onViewRecurrences,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

// Section Header Component
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Finance Tip Card - Personalized tips based on financial situation
@Composable
fun FinanceTipCard(
    currentBalance: Long,
    projectedExpenses: Long,
    fixedExpenses: Long
) {
    val projectedBalance = currentBalance - projectedExpenses

    val tip = when {
        projectedBalance < 0 -> TipData(
            icon = Icons.Outlined.Warning,
            title = "Dica: Corte gastos não essenciais",
            message = "Suas despesas projetadas excedem seu saldo. Considere adiar compras não urgentes ou renegociar algumas contas.",
            color = Color(0xFFF44336)
        )
        projectedBalance < currentBalance * 0.1 -> TipData(
            icon = Icons.Outlined.Savings,
            title = "Dica: Cuidado com o orçamento",
            message = "Sobrará menos de 10% do seu saldo. Evite gastos extras este mês.",
            color = Color(0xFFFF9800)
        )
        fixedExpenses > currentBalance * 0.5 -> TipData(
            icon = Icons.AutoMirrored.Outlined.TrendingDown,
            title = "Dica: Revise despesas fixas",
            message = "Suas despesas fixas representam mais de 50% do saldo. Considere renegociar contratos.",
            color = Color(0xFFFF9800)
        )
        projectedBalance > currentBalance * 0.3 -> TipData(
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            title = "Dica: Invista seu excedente! 📈",
            message = "Você terá mais de 30% de sobra! Que tal investir em renda fixa ou criar uma reserva de emergência?",
            color = Color(0xFF4CAF50)
        )
        else -> TipData(
            icon = Icons.Outlined.Lightbulb,
            title = "Dica: Continue assim! 💪",
            message = "Suas finanças estão equilibradas. Mantenha o controle e evite compras por impulso.",
            color = Color(0xFF2196F3)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = tip.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = tip.icon,
                contentDescription = null,
                tint = tip.color,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tip.color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private data class TipData(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val color: Color
)

@Composable
fun MonthSelector(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
            }
            Text(
                text = formatMonthYear(month, year),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
            }
        }
    }
}

@Composable
fun DashboardBoxes(
    dashboardData: com.example.gerenciadorfinanceiro.domain.usecase.DashboardData
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First Row: Current Balance and Fixed Expenses
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Current Balance Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Saldo Atual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dashboardData.currentBalance.toReais(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dashboardData.currentBalance >= 0)
                            Color(0xFF4CAF50)
                        else
                            Color(0xFFF44336),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Fixed Expenses Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Despesas Fixas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dashboardData.fixedExpenses.toReais(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Second Row: Credit Card Bills and Projected Expenses
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Credit Card Bills Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFB74D).copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Faturas Cartão",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dashboardData.creditCardBills.toReais(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 4. Projected Expenses Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Desp. Projetadas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dashboardData.projectedExpenses.toReais(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Third Row: Projected Balance (full width) with health status
        // Calculate: Current Balance - Projected Expenses
        val projectedBalance = dashboardData.currentBalance - dashboardData.projectedExpenses

        // Determine health status
        val healthStatus = when {
            projectedBalance < 0 -> HealthStatus.CRITICAL
            dashboardData.currentBalance <= 0 -> HealthStatus.CRITICAL
            projectedBalance < dashboardData.currentBalance * 0.1 -> HealthStatus.WARNING
            projectedBalance < dashboardData.currentBalance * 0.3 -> HealthStatus.GOOD
            else -> HealthStatus.EXCELLENT
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = healthStatus.color.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Icon and labels
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Status icon
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = healthStatus.color.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = healthStatus.icon,
                                contentDescription = null,
                                tint = healthStatus.color,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Saldo Projetado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = healthStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = healthStatus.color
                        )
                    }
                }

                // Right side: Value
                Text(
                    text = projectedBalance.toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = healthStatus.color
                )
            }
        }
    }
}

@Composable
fun GeneralBalanceCard(
    totalBalance: Long,
    projectedBalance: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Saldo Atual",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = totalBalance.toReais(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )

            Text(
                text = "Saldo Projetado (após pagamentos)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = projectedBalance.toReais(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (projectedBalance >= 0)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MonthlyOverviewCard(
    income: Long,
    expenses: Long,
    balance: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumo do Mês",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Receitas",
                    value = income.toReais(),
                    color = Color(0xFF4CAF50)
                )
                SummaryItem(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    label = "Despesas",
                    value = expenses.toReais(),
                    color = Color(0xFFF44336)
                )
                SummaryItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Balanço",
                    value = balance.toReais(),
                    color = if (balance >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun ProjectionDetailsCard(
    projection: com.example.gerenciadorfinanceiro.domain.usecase.BalanceProjection
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Detalhes da Projeção",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProjectionRow(
                label = "Saldo atual",
                value = projection.currentBalance.toReais(),
                isPositive = true
            )
            ProjectionRow(
                label = "+ Receitas pendentes",
                value = projection.pendingIncome.toReais(),
                isPositive = true
            )
            ProjectionRow(
                label = "- Despesas pendentes",
                value = projection.pendingExpenses.toReais(),
                isPositive = false
            )
            ProjectionRow(
                label = "- Recorrências projetadas",
                value = projection.projectedRecurrenceExpenses.toReais(),
                isPositive = false
            )
            ProjectionRow(
                label = "- Faturas de cartão",
                value = projection.unpaidCreditCardBills.toReais(),
                isPositive = false
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "= Saldo projetado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = projection.projectedBalance.toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (projection.projectedBalance >= 0)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun ProjectionRow(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPositive)
                Color(0xFF4CAF50)
            else
                Color(0xFFF44336)
        )
    }
}

@Composable
fun AccountsCarousel(accounts: List<Account>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts, key = { it.id }) { account ->
            AccountMiniCard(account = account)
        }
    }
}

@Composable
fun AccountMiniCard(account: Account) {
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = account.bank.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.balance.toReais(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (account.balance >= 0)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun UpcomingBillCard(bill: CreditCardBill) {
    val dueDate = Instant.ofEpochMilli(bill.dueDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Fatura ${bill.month.toString().padStart(2, '0')}/${bill.year}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Vencimento: ${formatter.format(dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = bill.totalAmount.toReais(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun RecurrenceCard(projectedRecurrence: ProjectedRecurrence) {
    val recurrence = projectedRecurrence.recurrence
    val date = Instant.ofEpochMilli(projectedRecurrence.projectedDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("dd/MM")

    val isExpense = recurrence.type == TransactionType.EXPENSE
    val containerColor = if (isExpense)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val textColor = if (isExpense)
        MaterialTheme.colorScheme.error
    else
        Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isExpense) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = recurrence.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "${formatter.format(date)} • ${recurrence.category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = recurrence.amount.toReais(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun EmptyDashboardState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bem-vindo ao Gerenciador Financeiro!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Comece adicionando suas contas, transações e cartões de crédito para ver seu resumo financeiro aqui.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

