package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCategoryAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val creditCardItemRepository: CreditCardItemRepository,
    private val recurrenceRepository: RecurrenceRepository
) {
    operator fun invoke(
        month: Int,
        year: Int,
        filter: DataSourceFilter
    ): Flow<CategoryBreakdown> {
        val (startDate, endDate) = getMonthBounds(month, year)

        return when (filter) {
            DataSourceFilter.ALL -> {
                combine(
                    transactionRepository.getCategoryTotals(startDate, endDate, TransactionType.EXPENSE),
                    transactionRepository.getCategoryTotals(startDate, endDate, TransactionType.INCOME),
                    creditCardItemRepository.getCategoryTotalsForMonth(month, year),
                    recurrenceRepository.getActiveRecurrences()
                ) { expenseTotals, incomeTotals, cardTotals, recurrences ->
                    val expenseMap = expenseTotals.associate { it.category to it.total }.toMutableMap()
                    val incomeMap = incomeTotals.associate { it.category to it.total }.toMutableMap()

                    cardTotals.forEach { cardTotal ->
                        expenseMap[cardTotal.category] =
                            expenseMap.getOrDefault(cardTotal.category, 0L) + cardTotal.total
                    }

                    recurrences.filter { it.type == TransactionType.EXPENSE }.forEach { rec ->
                        expenseMap[rec.category] =
                            expenseMap.getOrDefault(rec.category, 0L) + rec.amount
                    }

                    recurrences.filter { it.type == TransactionType.INCOME }.forEach { rec ->
                        incomeMap[rec.category] =
                            incomeMap.getOrDefault(rec.category, 0L) + rec.amount
                    }

                    val totalExpenses = expenseMap.values.sum()
                    val totalIncome = incomeMap.values.sum()

                    val expenses = expenseMap.map { (category, amount) ->
                        CategoryAnalytics(
                            category = category,
                            amount = amount,
                            transactionCount = 1,
                            percentage = if (totalExpenses > 0) (amount.toFloat() / totalExpenses * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    val income = incomeMap.map { (category, amount) ->
                        CategoryAnalytics(
                            category = category,
                            amount = amount,
                            transactionCount = 1,
                            percentage = if (totalIncome > 0) (amount.toFloat() / totalIncome * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    CategoryBreakdown(
                        expenses = expenses,
                        income = income,
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome
                    )
                }
            }

            DataSourceFilter.TRANSACTIONS -> {
                combine(
                    transactionRepository.getCategoryTotals(startDate, endDate, TransactionType.EXPENSE),
                    transactionRepository.getCategoryTotals(startDate, endDate, TransactionType.INCOME)
                ) { expenseTotals, incomeTotals ->
                    val totalExpenses = expenseTotals.sumOf { it.total }
                    val totalIncome = incomeTotals.sumOf { it.total }

                    val expenses = expenseTotals.map { total ->
                        CategoryAnalytics(
                            category = total.category,
                            amount = total.total,
                            transactionCount = total.count,
                            percentage = if (totalExpenses > 0) (total.total.toFloat() / totalExpenses * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    val income = incomeTotals.map { total ->
                        CategoryAnalytics(
                            category = total.category,
                            amount = total.total,
                            transactionCount = total.count,
                            percentage = if (totalIncome > 0) (total.total.toFloat() / totalIncome * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    CategoryBreakdown(
                        expenses = expenses,
                        income = income,
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome
                    )
                }
            }

            DataSourceFilter.CREDIT_CARDS -> {
                creditCardItemRepository.getCategoryTotalsForMonth(month, year).map { cardTotals ->
                    val totalExpenses = cardTotals.sumOf { it.total }

                    val expenses = cardTotals.map { total ->
                        CategoryAnalytics(
                            category = total.category,
                            amount = total.total,
                            transactionCount = total.count,
                            percentage = if (totalExpenses > 0) (total.total.toFloat() / totalExpenses * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    CategoryBreakdown(
                        expenses = expenses,
                        income = emptyList(),
                        totalExpenses = totalExpenses,
                        totalIncome = 0L
                    )
                }
            }

            DataSourceFilter.RECURRENCES -> {
                recurrenceRepository.getActiveRecurrences().map { recurrences ->
                    val expenseMap = mutableMapOf<com.example.gerenciadorfinanceiro.domain.model.Category, Long>()
                    val incomeMap = mutableMapOf<com.example.gerenciadorfinanceiro.domain.model.Category, Long>()

                    recurrences.filter { it.type == TransactionType.EXPENSE }.forEach { rec ->
                        expenseMap[rec.category] = expenseMap.getOrDefault(rec.category, 0L) + rec.amount
                    }

                    recurrences.filter { it.type == TransactionType.INCOME }.forEach { rec ->
                        incomeMap[rec.category] = incomeMap.getOrDefault(rec.category, 0L) + rec.amount
                    }

                    val totalExpenses = expenseMap.values.sum()
                    val totalIncome = incomeMap.values.sum()

                    val expenses = expenseMap.map { (category, amount) ->
                        CategoryAnalytics(
                            category = category,
                            amount = amount,
                            transactionCount = 1,
                            percentage = if (totalExpenses > 0) (amount.toFloat() / totalExpenses * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    val income = incomeMap.map { (category, amount) ->
                        CategoryAnalytics(
                            category = category,
                            amount = amount,
                            transactionCount = 1,
                            percentage = if (totalIncome > 0) (amount.toFloat() / totalIncome * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    CategoryBreakdown(
                        expenses = expenses,
                        income = income,
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome
                    )
                }
            }
        }
    }
}
