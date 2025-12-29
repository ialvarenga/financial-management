package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DashboardSummary(
    val totalBalance: Long = 0,
    val totalUnpaidBills: Long = 0,
    val monthlyIncome: Long = 0,
    val monthlyExpenses: Long = 0,
    val monthlyBalance: Long = 0
)

class GetDashboardSummaryUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardBillRepository: CreditCardBillRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Gets a summary of the dashboard for the given month/year
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of DashboardSummary
     */
    operator fun invoke(month: Int, year: Int): Flow<DashboardSummary> {
        val (startDate, endDate) = getMonthBounds(month, year)

        return combine(
            accountRepository.getTotalBalance(),
            creditCardBillRepository.getTotalUnpaidAmount(),
            transactionRepository.getTotalByDateRangeAndType(startDate, endDate, TransactionType.INCOME),
            transactionRepository.getTotalByDateRangeAndType(startDate, endDate, TransactionType.EXPENSE)
        ) { totalBalance, unpaidBills, income, expenses ->
            DashboardSummary(
                totalBalance = totalBalance,
                totalUnpaidBills = unpaidBills,
                monthlyIncome = income,
                monthlyExpenses = expenses,
                monthlyBalance = income - expenses
            )
        }
    }
}

