package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Converts a single-purchase credit card item into an installment purchase.
 *
 * Use case: When importing CSV bills, some items don't have installment info.
 * The user can then edit the item and specify "this is installment 3 of 10",
 * and this use case will update the current item and create the remaining
 * installments (4-10) in future bills.
 */
class ConvertToInstallmentUseCase @Inject constructor(
    private val billRepository: CreditCardBillRepository,
    private val itemRepository: CreditCardItemRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase
) {
    /**
     * Converts a single-purchase item into an installment purchase.
     *
     * @param itemId The ID of the item to convert
     * @param currentInstallment Which installment this item represents (e.g., 3 for "3 of 10")
     * @param totalInstallments Total number of installments (e.g., 10 for "3 of 10")
     * @return List of created item IDs (excluding the updated original item)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if item not found or already part of installment group
     */
    suspend operator fun invoke(
        itemId: Long,
        currentInstallment: Int,
        totalInstallments: Int
    ): List<Long> {
        require(currentInstallment in 1..totalInstallments) {
            "Current installment ($currentInstallment) must be between 1 and total ($totalInstallments)"
        }
        require(totalInstallments in 2..24) {
            "Total installments must be between 2 and 24"
        }

        val item = itemRepository.getById(itemId)
            ?: throw IllegalStateException("Item not found")

        if (item.installmentGroupId != null) {
            throw IllegalStateException("Item is already part of an installment group")
        }

        if (item.totalInstallments > 1) {
            throw IllegalStateException("Item is already an installment purchase")
        }

        val bill = billRepository.getById(item.creditCardBillId)
            ?: throw IllegalStateException("Bill not found")

        // Generate unique group ID for linking installments
        val installmentGroupId = UUID.randomUUID().toString()

        // Update the current item with installment info
        val updatedItem = item.copy(
            installmentNumber = currentInstallment,
            totalInstallments = totalInstallments,
            installmentGroupId = installmentGroupId
        )
        itemRepository.update(updatedItem)

        // Create remaining installments (currentInstallment + 1 to totalInstallments)
        val createdItemIds = mutableListOf<Long>()
        val remainingInstallments = (currentInstallment + 1)..totalInstallments

        for (installmentNumber in remainingInstallments) {
            // Calculate which bill this installment belongs to
            // Each subsequent installment goes to the next month's bill
            val monthsOffset = installmentNumber - currentInstallment
            val billDate = LocalDate.of(bill.year, bill.month, 1)
                .plusMonths(monthsOffset.toLong())

            // Get or create the bill for this month
            val targetBill = getOrCreateBillUseCase(
                bill.creditCardId,
                billDate.monthValue,
                billDate.year
            )

            // Create the installment item
            val newItem = CreditCardItem(
                creditCardBillId = targetBill.id,
                category = item.category,
                description = item.description,
                amount = item.amount, // Same amount per installment
                purchaseDate = item.purchaseDate,
                installmentNumber = installmentNumber,
                totalInstallments = totalInstallments,
                installmentGroupId = installmentGroupId
            )

            val newItemId = itemRepository.insert(newItem)
            createdItemIds.add(newItemId)

            // Update bill total
            val totalBillAmount = itemRepository.getTotalAmountByBill(targetBill.id)
            billRepository.updateTotalAmount(targetBill.id, totalBillAmount)
        }

        return createdItemIds
    }
}
