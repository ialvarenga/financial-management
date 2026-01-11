package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import javax.inject.Inject

class UpdateCreditCardItemUseCase @Inject constructor(
    private val itemRepository: CreditCardItemRepository,
    private val billRepository: CreditCardBillRepository
) {
    /**
     * Updates a credit card item and recalculates the bill's total amount.
     * If the category changed and the item belongs to an installment group,
     * all items in the group will be updated with the new category.
     * @param item The credit card item to update
     */
    suspend operator fun invoke(item: CreditCardItem) {
        // Get the original item to check for category changes
        val originalItem = itemRepository.getById(item.id)

        Log.d("UpdateCreditCardItem", "Original category: ${originalItem?.category}, New category: ${item.category}")
        Log.d("UpdateCreditCardItem", "InstallmentGroupId: ${item.installmentGroupId}")

        // Update the item
        itemRepository.update(item)

        // If category changed and item belongs to an installment group, update all items in the group
        if (originalItem != null &&
            originalItem.category != item.category &&
            item.installmentGroupId != null
        ) {
            Log.d("UpdateCreditCardItem", "Updating all items in group ${item.installmentGroupId} to category ${item.category}")
            itemRepository.updateCategoryByInstallmentGroup(item.installmentGroupId, item.category)
        } else {
            Log.d("UpdateCreditCardItem", "Not updating group. OriginalItem: ${originalItem != null}, CategoryChanged: ${originalItem?.category != item.category}, HasGroup: ${item.installmentGroupId != null}")
        }

        // Update the bill's total amount
        updateBillTotalAmount(item.creditCardBillId)
    }

    private suspend fun updateBillTotalAmount(billId: Long) {
        val totalAmount = itemRepository.getTotalAmountByBill(billId)
        billRepository.updateTotalAmount(billId, totalAmount)
    }
}
