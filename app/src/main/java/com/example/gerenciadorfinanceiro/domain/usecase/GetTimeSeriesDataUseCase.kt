package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetTimeSeriesDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        currentMonth: Int,
        currentYear: Int,
        timeRange: TimeRangeFilter
    ): Flow<TimeSeriesData> {
        val monthsToShow = when (timeRange) {
            TimeRangeFilter.THREE_MONTHS -> 3
            TimeRangeFilter.SIX_MONTHS -> 6
            TimeRangeFilter.TWELVE_MONTHS -> 12
        }

        val months = mutableListOf<Pair<Int, Int>>()
        var month = currentMonth
        var year = currentYear

        for (i in 0 until monthsToShow) {
            months.add(0, month to year)
            month--
            if (month == 0) {
                month = 12
                year--
            }
        }

        val flows = months.map { (m, y) ->
            val (startDate, endDate) = getMonthBounds(m, y)
            combine(
                transactionRepository.getTotalByDateRangeAndType(startDate, endDate, TransactionType.INCOME),
                transactionRepository.getTotalByDateRangeAndType(startDate, endDate, TransactionType.EXPENSE)
            ) { income, expenses ->
                val timestamp = LocalDate.of(y, m, 1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                TimeSeriesDataPoint(
                    month = m,
                    year = y,
                    income = income,
                    expenses = expenses,
                    balance = income - expenses,
                    timestamp = timestamp
                )
            }
        }

        return combine(flows) { dataPoints ->
            val maxIncome = dataPoints.maxOfOrNull { it.income } ?: 0L
            val maxExpense = dataPoints.maxOfOrNull { it.expenses } ?: 0L
            val maxBalance = dataPoints.maxOfOrNull { it.balance } ?: 0L
            val minBalance = dataPoints.minOfOrNull { it.balance } ?: 0L

            TimeSeriesData(
                dataPoints = dataPoints.toList(),
                maxIncome = maxIncome,
                maxExpense = maxExpense,
                maxBalance = maxBalance,
                minBalance = minBalance
            )
        }
    }
}
