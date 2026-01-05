package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPaymentMethodAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurrenceRepository: RecurrenceRepository
) {
    operator fun invoke(month: Int, year: Int, filter: DataSourceFilter): Flow<PaymentMethodBreakdown> {
        return when (filter) {
            DataSourceFilter.TRANSACTIONS, DataSourceFilter.ALL -> {
                val (startDate, endDate) = getMonthBounds(month, year)

                transactionRepository.getPaymentMethodTotals(startDate, endDate).map { methodTotals ->
                    val total = methodTotals.sumOf { it.total }

                    val methods = methodTotals.map { methodTotal ->
                        PaymentMethodAnalytics(
                            method = methodTotal.paymentMethod,
                            amount = methodTotal.total,
                            transactionCount = methodTotal.count,
                            percentage = if (total > 0) (methodTotal.total.toFloat() / total * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    PaymentMethodBreakdown(
                        methods = methods,
                        total = total
                    )
                }
            }

            DataSourceFilter.RECURRENCES -> {
                recurrenceRepository.getActiveRecurrences().map { recurrences ->
                    val methodMap = mutableMapOf<com.example.gerenciadorfinanceiro.domain.model.PaymentMethod, Long>()

                    recurrences.forEach { recurrence ->
                        methodMap[recurrence.paymentMethod] =
                            methodMap.getOrDefault(recurrence.paymentMethod, 0L) + recurrence.amount
                    }

                    val total = methodMap.values.sum()

                    val methods = methodMap.map { (method, amount) ->
                        PaymentMethodAnalytics(
                            method = method,
                            amount = amount,
                            transactionCount = recurrences.count { it.paymentMethod == method },
                            percentage = if (total > 0) (amount.toFloat() / total * 100) else 0f
                        )
                    }.sortedByDescending { it.amount }

                    PaymentMethodBreakdown(
                        methods = methods,
                        total = total
                    )
                }
            }

            DataSourceFilter.CREDIT_CARDS -> {
                // Credit card items don't have payment methods, return empty
                flowOf(PaymentMethodBreakdown(methods = emptyList(), total = 0L))
            }
        }
    }
}
