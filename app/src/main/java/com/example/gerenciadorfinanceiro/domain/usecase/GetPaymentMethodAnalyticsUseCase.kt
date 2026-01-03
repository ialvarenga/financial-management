package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPaymentMethodAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<PaymentMethodBreakdown> {
        val (startDate, endDate) = getMonthBounds(month, year)

        return transactionRepository.getPaymentMethodTotals(startDate, endDate).map { methodTotals ->
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
}
