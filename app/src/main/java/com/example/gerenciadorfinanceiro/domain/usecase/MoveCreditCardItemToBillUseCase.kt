package com.example.gerenciadorfinanceiro.domain.usecase

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import javax.inject.Inject

class MoveCreditCardItemToBillUseCase @Inject constructor(
    private val itemRepository: CreditCardItemRepository,
    private val billRepository: CreditCardBillRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val database: AppDatabase
) {
    /**
     * Moves a credit card item (and its installment group if applicable) to a different bill.
     *
     * @param itemId The ID of the item to move
     * @param targetBillId The ID of the target bill
     * @return Result indicating success or failure with error message
     */
    suspend operator fun invoke(itemId: Long, targetBillId: Long): Result<Unit> {
        return try {
            // Load the item
            val item = itemRepository.getById(itemId)
                ?: return Result.failure(Exception("Item não encontrado"))

            // If already in target bill, no-op
            if (item.creditCardBillId == targetBillId) {
                return Result.success(Unit)
            }

            // Load target bill
            val targetBill = billRepository.getById(targetBillId)
                ?: return Result.failure(Exception("Fatura de destino não encontrada"))

            // Validate target bill is OPEN
            if (targetBill.status != BillStatus.OPEN) {
                return Result.failure(Exception("Fatura de destino deve estar aberta"))
            }

            // Store original bill ID for total recalculation
            val originalBillId = item.creditCardBillId

            // Execute move operation within transaction for atomicity
            database.withTransaction {
                val affectedBillIds = mutableSetOf(originalBillId)

                if (item.installmentGroupId != null) {
                    // Handle installment group - move all installments together.
                    // Every installment's original bill (not just the one passed into
                    // invoke()) and every new bill it lands on needs its total recalculated.
                    affectedBillIds += moveInstallmentGroup(item.installmentGroupId, targetBill)
                } else {
                    // Handle single item
                    val updatedItem = item.copy(creditCardBillId = targetBillId)
                    itemRepository.update(updatedItem)
                    affectedBillIds.add(targetBillId)
                }

                affectedBillIds.forEach { billId ->
                    recalculateBillTotal(billId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Moves all items in an installment group to new bills based on the target bill.
     * Each installment is shifted to maintain the installment sequence.
     *
     * @return the set of every bill ID touched by the move (each installment's original
     * bill plus its new bill), so callers can recalculate all of their totals.
     */
    private suspend fun moveInstallmentGroup(groupId: String, targetBill: com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill): Set<Long> {
        // Load all items in the installment group
        val items = itemRepository.getItemsByInstallmentGroupSync(groupId)
        if (items.isEmpty()) return emptySet()

        // Sort by installment number to maintain order
        val sortedItems = items.sortedBy { it.installmentNumber }

        // Find the first installment to use as reference
        val firstItem = sortedItems.first()
        val firstItemBill = billRepository.getById(firstItem.creditCardBillId)
            ?: throw Exception("Fatura original não encontrada")

        // Calculate month difference between first item's current bill and target bill
        val currentMonthTotal = firstItemBill.year * 12 + firstItemBill.month
        val targetMonthTotal = targetBill.year * 12 + targetBill.month
        val monthShift = targetMonthTotal - currentMonthTotal

        val affectedBillIds = mutableSetOf<Long>()

        // Move each installment
        for (item in sortedItems) {
            val currentItemBill = billRepository.getById(item.creditCardBillId)
                ?: throw Exception("Fatura do item não encontrada")
            affectedBillIds.add(currentItemBill.id)

            // Calculate new month/year based on shift
            val currentItemMonthTotal = currentItemBill.year * 12 + currentItemBill.month
            val newMonthTotal = currentItemMonthTotal + monthShift
            val newYear = newMonthTotal / 12
            val newMonth = newMonthTotal % 12
            val adjustedMonth = if (newMonth == 0) 12 else newMonth
            val adjustedYear = if (newMonth == 0) newYear - 1 else newYear

            // Get or create bill for this month/year
            val newBill = getOrCreateBillUseCase(targetBill.creditCardId, adjustedMonth, adjustedYear)

            // Validate new bill is OPEN
            if (newBill.status != BillStatus.OPEN) {
                throw Exception("Não é possível mover parcela ${item.installmentNumber} - fatura de ${adjustedMonth}/${adjustedYear} não está aberta")
            }

            // Update item with new bill ID
            val updatedItem = item.copy(creditCardBillId = newBill.id)
            itemRepository.update(updatedItem)
            affectedBillIds.add(newBill.id)
        }

        return affectedBillIds
    }

    /**
     * Recalculates the total amount for a bill based on its items.
     */
    private suspend fun recalculateBillTotal(billId: Long) {
        val totalAmount = itemRepository.getTotalAmountByBill(billId)
        billRepository.updateTotalAmount(billId, totalAmount)
    }
}
