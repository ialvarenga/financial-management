package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class CreateInstallmentPurchaseUseCase @Inject constructor(
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
     * @param startMonth Month of the first bill (1-12)
     * @param startYear Year of the first bill
     * @return List of created item IDs
     */
    suspend operator fun invoke(
        creditCardId: Long,
        description: String,
        totalAmount: Long,
        category: Category,
        purchaseDate: Long,
        numberOfInstallments: Int,
        startMonth: Int,
        startYear: Int
    ): List<Long> {
        require(numberOfInstallments in 2..12) { "Number of installments must be between 2 and 12" }
        require(totalAmount > 0) { "Total amount must be greater than 0" }

        // Calculate installment amount (distribute remainder to first installment)
        val baseInstallmentAmount = totalAmount / numberOfInstallments
        val remainder = totalAmount % numberOfInstallments

        // Generate unique group ID for linking installments
        val installmentGroupId = UUID.randomUUID().toString()

        // Create items for each installment
        val itemIds = mutableListOf<Long>()

        for (installmentNumber in 1..numberOfInstallments) {
            // Calculate which bill this installment belongs to
            // Start from the provided startMonth/startYear and add months for each installment
            val billDate = LocalDate.of(startYear, startMonth, 1)
                .plusMonths((installmentNumber - 1).toLong())

            // Get or create the bill for this month
            val bill = getOrCreateBillUseCase(
                creditCardId,
                billDate.monthValue,
                billDate.year
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
}
