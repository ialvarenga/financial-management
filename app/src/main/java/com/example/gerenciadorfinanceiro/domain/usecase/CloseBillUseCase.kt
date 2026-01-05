package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CloseBillUseCase @Inject constructor(
    private val billRepository: CreditCardBillRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase
) {
    /**
     * Closes the bill for the given credit card if it's the closing date
     * @param creditCard The credit card to check
     * @param date The date to check against (defaults to today)
     * @return true if a bill was closed, false otherwise
     */
    suspend operator fun invoke(creditCard: CreditCard, date: LocalDate = LocalDate.now()): Boolean {
        try {
            // Check if today matches the closing day
            if (date.dayOfMonth != creditCard.closingDay) {
                Log.d(TAG, "Skipping card ${creditCard.name} - today (${date.dayOfMonth}) is not closing day (${creditCard.closingDay})")
                return false
            }

            // Get the bill for the current month
            val currentBill = billRepository.getBillByCardAndMonth(
                creditCardId = creditCard.id,
                month = date.monthValue,
                year = date.year
            )

            if (currentBill == null) {
                Log.d(TAG, "No bill found for ${creditCard.name} for ${date.monthValue}/${date.year}")
                return false
            }

            // Only close if the bill is currently OPEN
            if (currentBill.status != BillStatus.OPEN) {
                Log.d(TAG, "Bill for ${creditCard.name} is already ${currentBill.status}, skipping")
                return false
            }

            // Close the current bill
            billRepository.updateStatus(currentBill.id, BillStatus.CLOSED)
            Log.i(TAG, "Closed bill ${currentBill.id} for card ${creditCard.name} (${date.monthValue}/${date.year})")

            // Create the next month's bill
            val nextMonth = date.plusMonths(1)
            getOrCreateBillUseCase(creditCard.id, nextMonth.monthValue, nextMonth.year)
            Log.i(TAG, "Created bill for next month ${nextMonth.monthValue}/${nextMonth.year} for card ${creditCard.name}")

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error closing bill for card ${creditCard.name}", e)
            return false
        }
    }

    /**
     * Closes bills for all cards that have their closing date today
     * @param creditCards List of credit cards to check
     * @param date The date to check against (defaults to today)
     * @return Number of bills closed
     */
    suspend fun closeBillsForCards(
        creditCards: List<CreditCard>,
        date: LocalDate = LocalDate.now()
    ): Int {
        var closedCount = 0
        creditCards.forEach { card ->
            if (invoke(card, date)) {
                closedCount++
            }
        }
        Log.i(TAG, "Closed $closedCount bills out of ${creditCards.size} cards checked")
        return closedCount
    }

    /**
     * Closes overdue bills - bills that should have been closed in the past but are still OPEN.
     * This handles cases where bills were missed (app not running, errors, etc.)
     * @param creditCard The credit card to check
     * @param today The current date (defaults to today)
     * @return Number of bills closed
     */
    suspend fun closeOverdueBills(creditCard: CreditCard, today: LocalDate = LocalDate.now()): Int {
        try {
            var closedCount = 0
            val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Get all OPEN bills for this card
            val openBills = billRepository.getBillsByStatus(BillStatus.OPEN)
                .first()
                .filter { it.creditCardId == creditCard.id }

            for (bill in openBills) {
                // Check if the bill's closing date is in the past
                if (bill.closingDate < todayMillis) {
                    // Close this overdue bill
                    billRepository.updateStatus(bill.id, BillStatus.CLOSED)
                    Log.i(TAG, "Closed overdue bill ${bill.id} for card ${creditCard.name} (${bill.month}/${bill.year})")
                    closedCount++

                    // Ensure next month's bill exists
                    val nextMonth = LocalDate.of(bill.year, bill.month, 1).plusMonths(1)
                    getOrCreateBillUseCase(creditCard.id, nextMonth.monthValue, nextMonth.year)
                    Log.i(TAG, "Ensured bill exists for ${nextMonth.monthValue}/${nextMonth.year} for card ${creditCard.name}")
                }
            }

            if (closedCount > 0) {
                Log.i(TAG, "Closed $closedCount overdue bills for card ${creditCard.name}")
            }

            return closedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error closing overdue bills for card ${creditCard.name}", e)
            return 0
        }
    }

    /**
     * Closes overdue bills for all cards.
     * Should be called on app startup or worker execution to catch any missed closures.
     * @param creditCards List of credit cards to check
     * @param today The current date (defaults to today)
     * @return Number of bills closed
     */
    suspend fun closeOverdueBillsForCards(
        creditCards: List<CreditCard>,
        today: LocalDate = LocalDate.now()
    ): Int {
        var totalClosed = 0
        creditCards.forEach { card ->
            totalClosed += closeOverdueBills(card, today)
        }
        if (totalClosed > 0) {
            Log.i(TAG, "Closed $totalClosed total overdue bills across ${creditCards.size} cards")
        }
        return totalClosed
    }

    companion object {
        private const val TAG = "CloseBillUseCase"
    }
}
