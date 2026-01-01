package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class BalanceProjection(
    val currentBalance: Long = 0,
    val pendingIncome: Long = 0,
    val pendingExpenses: Long = 0,
    val projectedRecurrenceExpenses: Long = 0,
    val unpaidCreditCardBills: Long = 0,
    val projectedBalance: Long = 0,
    val upcomingBills: List<CreditCardBill> = emptyList()
)

class GetBalanceAfterPaymentsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardBillRepository: CreditCardBillRepository,
    private val transactionRepository: TransactionRepository,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase
) {
    /**
     * Calculates the projected balance after all pending payments
     * for the given month/year, considering:
     * - Current account balances
     * - Pending transactions (not yet completed)
     * - Projected recurrences for the month
     * - Unpaid credit card bills due within the selected month
     *
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of BalanceProjection
     */
    operator fun invoke(month: Int, year: Int): Flow<BalanceProjection> {
        val (startDate, endDate) = getMonthBounds(month, year)

        return combine(
            accountRepository.getTotalBalance(),
            transactionRepository.getByDateRangeAndStatus(startDate, endDate, TransactionStatus.PENDING),
            getMonthlyExpensesUseCase(month, year),
            creditCardBillRepository.getUnpaidBillsInDateRange(startDate, endDate)
        ) { totalBalance, pendingTransactions, projectedRecurrences, unpaidBills ->

            val pendingIncome = pendingTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val pendingExpenses = pendingTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            // Calculate projected recurrence expenses (only expenses that haven't been confirmed yet)
            val projectedRecurrenceExpenses = projectedRecurrences
                .filter { it.recurrence.type == TransactionType.EXPENSE }
                .sumOf { it.recurrence.amount }

            val totalUnpaidBills = unpaidBills.sumOf { it.totalAmount }

            // Projected balance = Current balance
            //                   + Pending income (will increase balance)
            //                   - Pending expenses (will decrease balance)
            //                   - Projected recurrence expenses (future deductions)
            //                   - Unpaid credit card bills (future deductions)
            val projectedBalance = totalBalance + pendingIncome - pendingExpenses - projectedRecurrenceExpenses - totalUnpaidBills

            BalanceProjection(
                currentBalance = totalBalance,
                pendingIncome = pendingIncome,
                pendingExpenses = pendingExpenses,
                projectedRecurrenceExpenses = projectedRecurrenceExpenses,
                unpaidCreditCardBills = totalUnpaidBills,
                projectedBalance = projectedBalance,
                upcomingBills = unpaidBills.sortedBy { it.dueDate }
            )
        }
    }
}

