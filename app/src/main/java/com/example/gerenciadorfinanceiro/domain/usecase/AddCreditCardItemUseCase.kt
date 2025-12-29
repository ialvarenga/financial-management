package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import javax.inject.Inject

class AddCreditCardItemUseCase @Inject constructor(
    private val itemRepository: CreditCardItemRepository,
    private val billRepository: CreditCardBillRepository
) {
    /**
     * Adds a single item to a credit card bill and updates the bill's total amount
     * @param item The credit card item to add
     * @return The ID of the created item
     */
    suspend operator fun invoke(item: CreditCardItem): Long {
        // Insert the item
        val itemId = itemRepository.insert(item)

        // Update the bill's total amount
        updateBillTotalAmount(item.creditCardBillId)

        return itemId
    }

    private suspend fun updateBillTotalAmount(billId: Long) {
        val totalAmount = itemRepository.getTotalAmountByBill(billId)
        billRepository.updateTotalAmount(billId, totalAmount)
    }
}
