package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CreateCreditCardPurchaseUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val addCreditCardItemUseCase: AddCreditCardItemUseCase
) {
    suspend operator fun invoke(parsed: ParsedNotification, notificationKey: String): ProcessedNotification {
        val creditCard = if (parsed.lastFourDigits != null) {
            // Find by last 4 digits
            creditCardRepository.getByLastFourDigits(parsed.lastFourDigits)
                ?: throw IllegalStateException("No credit card found with last 4 digits: ${parsed.lastFourDigits}")
        } else {
            // For Nupay notifications without last 4 digits, find the Nubank credit card
            creditCardRepository.getByBank(com.example.gerenciadorfinanceiro.domain.model.Bank.NUBANK)
                ?: throw IllegalStateException("No Nubank credit card found for Nupay purchase")
        }

        // Determine the month/year from the purchase timestamp
        val purchaseDate = Instant.ofEpochMilli(parsed.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Get or create the bill for the purchase month
        var bill = getOrCreateBillUseCase(
            creditCardId = creditCard.id,
            month = purchaseDate.monthValue,
            year = purchaseDate.year
        )

        // If the bill is already closed, add the item to the next month's bill instead
        if (bill.status == BillStatus.CLOSED || bill.status == BillStatus.PAID) {
            val nextMonth = LocalDate.of(bill.year, bill.month, 1).plusMonths(1)
            bill = getOrCreateBillUseCase(
                creditCardId = creditCard.id,
                month = nextMonth.monthValue,
                year = nextMonth.year
            )
            Log.i(TAG, "Bill for ${purchaseDate.monthValue}/${purchaseDate.year} is ${bill.status}, " +
                    "adding item to next month's bill (${nextMonth.monthValue}/${nextMonth.year})")
        }

        // Create the credit card item
        val item = CreditCardItem(
            creditCardBillId = bill.id,
            category = Category.OTHER,
            description = parsed.description,
            amount = parsed.amount,
            purchaseDate = parsed.timestamp,
            installmentNumber = 1,
            totalInstallments = parsed.installments,
            installmentGroupId = null
        )

        val itemId = addCreditCardItemUseCase(item)

        return ProcessedNotification(
            notificationKey = notificationKey,
            source = parsed.source,
            notificationText = parsed.description,
            createdCreditCardItemId = itemId
        )
    }

    companion object {
        private const val TAG = "CreateCreditCardPurchaseUseCase"
    }
}
