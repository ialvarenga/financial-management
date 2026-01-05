package com.example.gerenciadorfinanceiro.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.Pie
import com.example.gerenciadorfinanceiro.domain.usecase.DataSourceFilter
import com.example.gerenciadorfinanceiro.domain.usecase.TimeRangeFilter
import com.example.gerenciadorfinanceiro.util.toReais
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análises") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                item {
                    MonthSelector(
                        selectedMonth = uiState.selectedMonth,
                        selectedYear = uiState.selectedYear,
                        onPreviousMonth = viewModel::selectPreviousMonth,
                        onNextMonth = viewModel::selectNextMonth
                    )
                }

                item {
                    DataSourceFilterRow(
                        selected = uiState.filterType,
                        onFilterSelected = viewModel::setFilter
                    )
                }

                item {
                    SectionHeader("Distribuição por Categoria")
                }

                if (uiState.categoryBreakdown.expenses.isNotEmpty()) {
                    item(key = "expenses_${uiState.filterType}_${uiState.categoryBreakdown.expenses.size}") {
                        CategoryDonutChart(
                            title = "Despesas",
                            items = uiState.categoryBreakdown.expenses.take(10),
                            total = uiState.categoryBreakdown.totalExpenses
                        )
                    }
                }

                if (uiState.categoryBreakdown.income.isNotEmpty()) {
                    item(key = "income_${uiState.filterType}_${uiState.categoryBreakdown.income.size}") {
                        CategoryDonutChart(
                            title = "Receitas",
                            items = uiState.categoryBreakdown.income.take(10),
                            total = uiState.categoryBreakdown.totalIncome
                        )
                    }
                }

                item {
                    SectionHeader("Tendências")
                }

                item {
                    TimeRangeFilterRow(
                        selected = uiState.timeRange,
                        onRangeSelected = viewModel::setTimeRange
                    )
                }

                if (uiState.timeSeriesData.dataPoints.isNotEmpty()) {
                    item {
                        TimeSeriesLineChart(
                            dataPoints = uiState.timeSeriesData.dataPoints
                        )
                    }
                }

                if (uiState.paymentMethodBreakdown.methods.isNotEmpty()) {
                    item {
                        SectionHeader("Métodos de Pagamento")
                    }
                    item(key = "payment_methods_${uiState.filterType}_${uiState.paymentMethodBreakdown.methods.size}") {
                        PaymentMethodDonutChart(
                            methods = uiState.paymentMethodBreakdown.methods,
                            total = uiState.paymentMethodBreakdown.total
                        )
                    }
                }

                if (uiState.accountBreakdown.accounts.isNotEmpty()) {
                    item {
                        SectionHeader("Saldo por Conta")
                    }
                    item {
                        AccountsCard(
                            accounts = uiState.accountBreakdown.accounts,
                            total = uiState.accountBreakdown.totalBalance
                        )
                    }
                }

                if (uiState.creditCardUtilization.cards.isNotEmpty()) {
                    item {
                        SectionHeader("Utilização de Cartões")
                    }
                    items(uiState.creditCardUtilization.cards) { card ->
                        CreditCardUtilizationItem(card)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
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
            IconButton(onClick = onPreviousMonth) {
                Text("<", style = MaterialTheme.typography.headlineSmall)
            }

            val monthName = java.time.Month.of(selectedMonth)
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(
                text = "$monthName $selectedYear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextMonth) {
                Text(">", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourceFilterRow(
    selected: DataSourceFilter,
    onFilterSelected: (DataSourceFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val filterOptions = mapOf(
        DataSourceFilter.ALL to "Todas",
        DataSourceFilter.TRANSACTIONS to "Transações",
        DataSourceFilter.CREDIT_CARDS to "Cartões",
        DataSourceFilter.RECURRENCES to "Recorrências"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = filterOptions[selected] ?: "Todas",
            onValueChange = {},
            readOnly = true,
            label = { Text("Fonte de Dados") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filterOptions.forEach { (filter, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun TimeRangeFilterRow(
    selected: TimeRangeFilter,
    onRangeSelected: (TimeRangeFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == TimeRangeFilter.THREE_MONTHS,
            onClick = { onRangeSelected(TimeRangeFilter.THREE_MONTHS) },
            label = { Text("3 meses") }
        )
        FilterChip(
            selected = selected == TimeRangeFilter.SIX_MONTHS,
            onClick = { onRangeSelected(TimeRangeFilter.SIX_MONTHS) },
            label = { Text("6 meses") }
        )
        FilterChip(
            selected = selected == TimeRangeFilter.TWELVE_MONTHS,
            onClick = { onRangeSelected(TimeRangeFilter.TWELVE_MONTHS) },
            label = { Text("12 meses") }
        )
    }
}

@Composable
fun SummaryCard(
    totalIncome: Long,
    totalExpenses: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Receitas:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    totalIncome.toReais(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Despesas:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    totalExpenses.toReais(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo:", style = MaterialTheme.typography.titleMedium)
                Text(
                    (totalIncome - totalExpenses).toReais(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (totalIncome - totalExpenses >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun CategoryDonutChart(
    title: String,
    items: List<com.example.gerenciadorfinanceiro.domain.usecase.CategoryAnalytics>,
    total: Long
) {
    if (items.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val pieData = items.map { item ->
                Pie(
                    label = item.category.displayName,
                    data = item.percentage.toDouble(),
                    color = item.category.color,
                    selectedColor = item.category.color.copy(alpha = 0.8f)
                )
            }

            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 16.dp),
                data = pieData,
                onPieClick = { },
                selectedScale = 1.1f,
                style = Pie.Style.Stroke(width = 100.dp),
                spaceDegree = 2f,
                selectedPaddingDegree = 4f
            )

            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(item.category.color)
                            )
                            Text(
                                text = item.category.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${item.amount.toReais()} (${String.format("%.1f%%", item.percentage)})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSeriesLineChart(
    dataPoints: List<com.example.gerenciadorfinanceiro.domain.usecase.TimeSeriesDataPoint>
) {
    if (dataPoints.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Receitas vs Despesas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val incomeValues = dataPoints.map { (it.income / 100.0) }
            val expenseValues = dataPoints.map { (it.expenses / 100.0) }
            val balanceValues = dataPoints.map { (it.balance / 100.0) }

            val lines = listOf(
                Line(
                    label = "Receitas",
                    values = incomeValues,
                    color = androidx.compose.ui.graphics.SolidColor(Color(0xFF10B981)),
                    curvedEdges = true,
                    drawStyle = DrawStyle.Stroke(width = 3.dp)
                ),
                Line(
                    label = "Despesas",
                    values = expenseValues,
                    color = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)),
                    curvedEdges = true,
                    drawStyle = DrawStyle.Stroke(width = 3.dp)
                ),
                Line(
                    label = "Saldo",
                    values = balanceValues,
                    color = androidx.compose.ui.graphics.SolidColor(Color(0xFF3B82F6)),
                    curvedEdges = true,
                    drawStyle = DrawStyle.Stroke(width = 2.dp)
                )
            )

            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                data = lines,
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 100L
                }),
                curvedEdges = true
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Receitas", Color(0xFF10B981))
                LegendItem("Despesas", Color(0xFFEF4444))
                LegendItem("Saldo", Color(0xFF3B82F6))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PaymentMethodDonutChart(
    methods: List<com.example.gerenciadorfinanceiro.domain.usecase.PaymentMethodAnalytics>,
    total: Long
) {
    if (methods.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val colors = listOf(
                Color(0xFF8B5CF6), // Vibrant Purple
                Color(0xFF10B981), // Emerald Green
                Color(0xFFF59E0B), // Amber
                Color(0xFFEF4444), // Red
                Color(0xFF3B82F6), // Blue
                Color(0xFFEC4899), // Pink
                Color(0xFF06B6D4), // Cyan
                Color(0xFF6366F1), // Indigo
                Color(0xFFF97316)  // Orange
            )

            val pieData = methods.mapIndexed { index, method ->
                Pie(
                    label = method.method.name,
                    data = method.percentage.toDouble(),
                    color = colors[index % colors.size],
                    selectedColor = colors[index % colors.size].copy(alpha = 0.8f)
                )
            }

            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 16.dp),
                data = pieData,
                onPieClick = { },
                selectedScale = 1.1f,
                style = Pie.Style.Stroke(width = 100.dp),
                spaceDegree = 2f,
                selectedPaddingDegree = 4f
            )

            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                methods.forEachIndexed { index, method ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(colors[index % colors.size])
                            )
                            Text(
                                text = method.method.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${method.amount.toReais()} (${String.format("%.1f%%", method.percentage)})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsCard(
    accounts: List<com.example.gerenciadorfinanceiro.domain.usecase.AccountAnalytics>,
    total: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            accounts.forEach { account ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(account.account.name)
                    Text(
                        account.balance.toReais(),
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun CreditCardUtilizationItem(
    utilization: com.example.gerenciadorfinanceiro.domain.usecase.CreditCardUtilization
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    utilization.creditCard.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.1f%%", utilization.utilizationPercentage)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        utilization.utilizationPercentage < 30 -> Color(0xFF4CAF50)
                        utilization.utilizationPercentage < 70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            LinearProgressIndicator(
                progress = { (utilization.utilizationPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    utilization.utilizationPercentage < 30 -> Color(0xFF4CAF50)
                    utilization.utilizationPercentage < 70 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Utilizado: ${utilization.used.toReais()}", style = MaterialTheme.typography.bodySmall)
                Text("Disponível: ${utilization.available.toReais()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
