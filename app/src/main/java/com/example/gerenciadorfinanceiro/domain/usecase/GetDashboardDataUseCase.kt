package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DashboardData(
    val currentBalance: Long = 0,  // Total balance across all accounts
    val fixedExpenses: Long = 0,  // Transactions with recurrenceId + unconfirmed recurrences
    val creditCardBills: Long = 0,  // Credit card bills due this month
    val projectedExpenses: Long = 0  // Pending transactions + recurrences + credit card bills
)

class GetDashboardDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardBillRepository: CreditCardBillRepository,
    private val transactionRepository: TransactionRepository,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase
) {
    /**
     * Calculates dashboard data for the given month/year:
     * 1. Current Balance - sum of all account balances
     * 2. Fixed Expenses - transactions with recurrenceId + unconfirmed projected recurrences
     * 3. Credit Card Bills - bills due within the selected month
     * 4. Projected Expenses - pending transactions + recurrences + credit card bills
     *
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of DashboardData
     */
    operator fun invoke(month: Int, year: Int): Flow<DashboardData> {
        val (startDate, endDate) = getMonthBounds(month, year)

        return combine(
            accountRepository.getTotalBalance(),
            transactionRepository.getByDateRangeAndStatus(startDate, endDate, TransactionStatus.PENDING),
            transactionRepository.getByDateRangeWithAccount(startDate, endDate),
            getMonthlyExpensesUseCase(month, year, excludeConfirmed = true),
            creditCardBillRepository.getUnpaidBillsInDateRange(startDate, endDate)
        ) { totalBalance, pendingTransactions, allTransactions, unconfirmedRecurrences, unpaidBills ->

            // Fixed expenses = transactions with recurrenceId + unconfirmed projected recurrences
            // 1. Sum of expense transactions that have a recurrenceId (confirmed recurrences)
            val confirmedRecurrenceExpenses = allTransactions
                .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.recurrenceId != null }
                .sumOf { it.transaction.amount }

            // 2. Sum of unconfirmed projected recurrences
            val unconfirmedRecurrenceExpenses = unconfirmedRecurrences
                .filter { it.recurrence.type == TransactionType.EXPENSE }
                .sumOf { it.recurrence.amount }

            // Total fixed expenses
            val totalFixedExpenses = confirmedRecurrenceExpenses + unconfirmedRecurrenceExpenses

            // Calculate pending transaction expenses (for projected expenses calculation)
            val pendingExpenses = pendingTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            // Calculate total credit card bills
            val totalCreditCardBills = unpaidBills.sumOf { it.totalAmount }

            // Projected expenses = pending expenses + unconfirmed recurrence expenses + credit card bills
            val projectedExpenses = pendingExpenses + unconfirmedRecurrenceExpenses + totalCreditCardBills

            DashboardData(
                currentBalance = totalBalance,
                fixedExpenses = totalFixedExpenses,
                creditCardBills = totalCreditCardBills,
                projectedExpenses = projectedExpenses
            )
        }
    }
}
