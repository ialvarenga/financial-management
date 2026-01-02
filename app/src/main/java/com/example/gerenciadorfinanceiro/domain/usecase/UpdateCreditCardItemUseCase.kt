package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import javax.inject.Inject

class UpdateCreditCardItemUseCase @Inject constructor(
    private val itemRepository: CreditCardItemRepository,
    private val billRepository: CreditCardBillRepository
) {
    /**
     * Updates a credit card item and recalculates the bill's total amount
     * @param item The credit card item to update
     */
    suspend operator fun invoke(item: CreditCardItem) {
        // Update the item
        itemRepository.update(item)

        // Update the bill's total amount
        updateBillTotalAmount(item.creditCardBillId)
    }

    private suspend fun updateBillTotalAmount(billId: Long) {
        val totalAmount = itemRepository.getTotalAmountByBill(billId)
        billRepository.updateTotalAmount(billId, totalAmount)
    }
}
