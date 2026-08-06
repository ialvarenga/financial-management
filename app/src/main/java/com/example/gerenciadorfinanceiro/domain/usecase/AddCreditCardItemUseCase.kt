package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
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
        val bill = billRepository.getById(item.creditCardBillId)
            ?: throw IllegalStateException("Fatura não encontrada")
        if (bill.status != BillStatus.OPEN) {
            throw IllegalStateException("Não é possível adicionar itens a uma fatura fechada ou paga")
        }

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
