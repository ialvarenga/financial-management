package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class CreateWalletPurchaseUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val addCreditCardItemUseCase: AddCreditCardItemUseCase
) {
    suspend operator fun invoke(parsed: ParsedNotification, notificationKey: String): ProcessedNotification {
        val lastFour = parsed.lastFourDigits
            ?: throw IllegalArgumentException("Missing last four digits for Google Wallet transaction")

        val creditCard = creditCardRepository.getByLastFourDigits(lastFour)
            ?: run {
                Log.i(TAG, "No credit card found with last 4 digits: $lastFour, creating placeholder")
                creditCardRepository.createPlaceholderCard(
                    lastFourDigits = lastFour,
                    name = "Card ending in $lastFour"
                )
            }

        val purchaseDate = Instant.ofEpochMilli(parsed.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val bill = getOrCreateBillUseCase(
            creditCardId = creditCard.id,
            month = purchaseDate.monthValue,
            year = purchaseDate.year
        )

        val item = CreditCardItem(
            creditCardBillId = bill.id,
            category = Category.OTHER,
            description = parsed.description,
            amount = parsed.amount,
            purchaseDate = parsed.timestamp
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
        private const val TAG = "CreateWalletPurchase"
    }
}
