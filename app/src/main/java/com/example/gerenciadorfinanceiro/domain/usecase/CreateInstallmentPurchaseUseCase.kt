package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.util.toLocalDate
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class CreateInstallmentPurchaseUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val billRepository: CreditCardBillRepository,
    private val itemRepository: CreditCardItemRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase
) {
    /**
     * Creates an installment purchase across multiple credit card bills
     * @param creditCardId The credit card to add the purchase to
     * @param description Description of the purchase
     * @param totalAmount Total purchase amount in cents
     * @param category Purchase category
     * @param purchaseDate When the purchase was made (epoch millis)
     * @param numberOfInstallments How many installments (2-12)
     * @return List of created item IDs
     */
    suspend operator fun invoke(
        creditCardId: Long,
        description: String,
        totalAmount: Long,
        category: Category,
        purchaseDate: Long,
        numberOfInstallments: Int
    ): List<Long> {
        require(numberOfInstallments in 2..12) { "Number of installments must be between 2 and 12" }
        require(totalAmount > 0) { "Total amount must be greater than 0" }

        // Get credit card info
        val creditCard = creditCardRepository.getById(creditCardId)
            ?: throw IllegalArgumentException("Credit card not found")

        // Calculate installment amount (distribute remainder to first installment)
        val baseInstallmentAmount = totalAmount / numberOfInstallments
        val remainder = totalAmount % numberOfInstallments

        // Generate unique group ID for linking installments
        val installmentGroupId = UUID.randomUUID().toString()

        // Convert purchase date to LocalDate
        val purchaseDateLocal = purchaseDate.toLocalDate()

        // Create items for each installment
        val itemIds = mutableListOf<Long>()

        for (installmentNumber in 1..numberOfInstallments) {
            // Calculate which bill this installment belongs to
            val billMonthYear = calculateBillMonthYear(
                purchaseDateLocal,
                creditCard.closingDay,
                installmentNumber
            )

            // Get or create the bill for this month
            val bill = getOrCreateBillUseCase(
                creditCardId,
                billMonthYear.first,
                billMonthYear.second
            )

            // Calculate amount for this installment (first one gets the remainder)
            val installmentAmount = if (installmentNumber == 1) {
                baseInstallmentAmount + remainder
            } else {
                baseInstallmentAmount
            }

            // Create the item
            val item = CreditCardItem(
                creditCardBillId = bill.id,
                category = category,
                description = "$description ($installmentNumber/$numberOfInstallments)",
                amount = installmentAmount,
                purchaseDate = purchaseDate,
                installmentNumber = installmentNumber,
                totalInstallments = numberOfInstallments,
                installmentGroupId = installmentGroupId
            )

            val itemId = itemRepository.insert(item)
            itemIds.add(itemId)

            // Update bill total
            val totalBillAmount = itemRepository.getTotalAmountByBill(bill.id)
            billRepository.updateTotalAmount(bill.id, totalBillAmount)
        }

        return itemIds
    }

    /**
     * Calculates which month/year a specific installment should be billed to
     * @param purchaseDate Date of purchase
     * @param closingDay Closing day of the credit card
     * @param installmentNumber Which installment (1-based)
     * @return Pair of (month, year)
     */
    private fun calculateBillMonthYear(
        purchaseDate: LocalDate,
        closingDay: Int,
        installmentNumber: Int
    ): Pair<Int, Int> {
        // If purchase is before or on closing day, it goes to current month's bill
        // Otherwise, it goes to next month's bill
        val firstBillDate = if (purchaseDate.dayOfMonth <= closingDay) {
            purchaseDate
        } else {
            purchaseDate.plusMonths(1)
        }

        // Add months for subsequent installments (installmentNumber - 1)
        val billDate = firstBillDate.plusMonths((installmentNumber - 1).toLong())

        return Pair(billDate.monthValue, billDate.year)
    }
}
