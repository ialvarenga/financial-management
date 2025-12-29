package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetOrCreateBillUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val billRepository: CreditCardBillRepository
) {
    /**
     * Gets an existing bill for the specified month/year or creates a new one
     * @param creditCardId The credit card ID
     * @param month Month (1-12)
     * @param year Year
     * @return The existing or newly created bill
     */
    suspend operator fun invoke(creditCardId: Long, month: Int, year: Int): CreditCardBill {
        // Try to get existing bill
        val existingBill = billRepository.getBillByCardAndMonth(creditCardId, month, year)
        if (existingBill != null) {
            return existingBill
        }

        // Get credit card to know closing/due days
        val creditCard = creditCardRepository.getById(creditCardId)
            ?: throw IllegalArgumentException("Credit card not found")

        // Create new bill
        return createBill(creditCard, month, year)
    }

    /**
     * Gets or creates the bill for the current month
     */
    suspend fun getCurrentMonthBill(creditCardId: Long): CreditCardBill {
        val now = LocalDate.now()
        return invoke(creditCardId, now.monthValue, now.year)
    }

    private suspend fun createBill(creditCard: CreditCard, month: Int, year: Int): CreditCardBill {
        // Calculate closing date (day of the month)
        val closingDate = LocalDate.of(year, month, creditCard.closingDay.coerceIn(1, 28))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Calculate due date (usually in the next month if after closing day)
        val dueDate = LocalDate.of(year, month, creditCard.dueDay.coerceIn(1, 28))
            .let {
                if (creditCard.dueDay < creditCard.closingDay) {
                    it.plusMonths(1)
                } else {
                    it
                }
            }
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val bill = CreditCardBill(
            creditCardId = creditCard.id,
            month = month,
            year = year,
            closingDate = closingDate,
            dueDate = dueDate,
            totalAmount = 0,
            status = BillStatus.OPEN
        )

        val billId = billRepository.insert(bill)
        return bill.copy(id = billId)
    }
}
