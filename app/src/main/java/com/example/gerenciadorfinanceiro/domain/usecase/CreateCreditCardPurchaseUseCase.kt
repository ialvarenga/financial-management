package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class CreateCreditCardPurchaseUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val addCreditCardItemUseCase: AddCreditCardItemUseCase
) {
    suspend operator fun invoke(parsed: ParsedNotification, notificationKey: String): ProcessedNotification {
        val lastFourDigits = parsed.lastFourDigits
            ?: throw IllegalArgumentException("Credit card purchase must have lastFourDigits")

        val creditCard = creditCardRepository.getByLastFourDigits(lastFourDigits)
            ?: throw IllegalStateException("No credit card found with last 4 digits: $lastFourDigits")

        // Determine the month/year from the purchase timestamp
        val purchaseDate = Instant.ofEpochMilli(parsed.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Get or create the bill for the purchase month
        val bill = getOrCreateBillUseCase(
            creditCardId = creditCard.id,
            month = purchaseDate.monthValue,
            year = purchaseDate.year
        )

        // Create the credit card item
        val item = CreditCardItem(
            creditCardBillId = bill.id,
            category = Category.OTHER,
            description = parsed.description,
            amount = parsed.amount,
            purchaseDate = parsed.timestamp,
            installmentNumber = 1,
            totalInstallments = 1,
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
}
