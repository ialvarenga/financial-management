package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MarkBillAsPaidUseCase @Inject constructor(
    private val billRepository: CreditCardBillRepository
) {
    suspend operator fun invoke(billId: Long): Result<Unit> {
        return try {
            val bill = billRepository.getById(billId)
                ?: return Result.failure(Exception("Bill not found"))

            if (bill.status == BillStatus.PAID) {
                Log.d(TAG, "Bill $billId is already marked as paid")
                return Result.success(Unit)
            }

            if (bill.status != BillStatus.CLOSED) {
                Log.w(TAG, "Bill $billId is not closed, marking as paid anyway")
            }

            billRepository.updateStatus(billId, BillStatus.PAID, System.currentTimeMillis())
            Log.i(TAG, "Successfully marked bill $billId as paid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking bill as paid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markLatestClosedBillAsPaid(): Result<Unit> {
        return try {
            val unpaidBills = billRepository.getUnpaidBills()
            val closedBill = unpaidBills.first()
                .firstOrNull { it.status == BillStatus.CLOSED }
                ?: return Result.failure(Exception("No closed unpaid bill found"))

            invoke(closedBill.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking latest closed bill as paid: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MarkBillAsPaidUseCase"
    }
}
