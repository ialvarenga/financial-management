package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod

data class CategoryAnalytics(
    val category: Category,
    val amount: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class CategoryBreakdown(
    val expenses: List<CategoryAnalytics> = emptyList(),
    val income: List<CategoryAnalytics> = emptyList(),
    val totalExpenses: Long = 0,
    val totalIncome: Long = 0
)

data class TimeSeriesDataPoint(
    val month: Int,
    val year: Int,
    val income: Long,
    val expenses: Long,
    val balance: Long,
    val timestamp: Long
)

data class TimeSeriesData(
    val dataPoints: List<TimeSeriesDataPoint> = emptyList(),
    val maxIncome: Long = 0,
    val maxExpense: Long = 0,
    val maxBalance: Long = 0,
    val minBalance: Long = 0
)

data class PaymentMethodAnalytics(
    val method: PaymentMethod,
    val amount: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class PaymentMethodBreakdown(
    val methods: List<PaymentMethodAnalytics> = emptyList(),
    val total: Long = 0
)

data class AccountAnalytics(
    val account: Account,
    val balance: Long,
    val percentage: Float
)

data class AccountBreakdown(
    val accounts: List<AccountAnalytics> = emptyList(),
    val totalBalance: Long = 0
)

data class CreditCardUtilization(
    val creditCard: CreditCard,
    val used: Long,
    val available: Long,
    val limit: Long,
    val utilizationPercentage: Float
)

data class CreditCardUtilizationData(
    val cards: List<CreditCardUtilization> = emptyList()
)

enum class DataSourceFilter {
    ALL,
    TRANSACTIONS,
    CREDIT_CARDS,
    RECURRENCES
}

enum class TimeRangeFilter {
    THREE_MONTHS,
    SIX_MONTHS,
    TWELVE_MONTHS
}
