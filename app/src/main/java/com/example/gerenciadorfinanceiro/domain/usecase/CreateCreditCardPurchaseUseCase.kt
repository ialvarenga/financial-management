package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.Bank
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
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
        val creditCard = findOrCreateCreditCard(parsed)

        val purchaseDate = Instant.ofEpochMilli(parsed.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        var bill = getOrCreateBillUseCase(
            creditCardId = creditCard.id,
            month = purchaseDate.monthValue,
            year = purchaseDate.year
        )

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

    private suspend fun findOrCreateCreditCard(parsed: ParsedNotification): CreditCard {
        if (parsed.lastFourDigits != null) {
            val card = creditCardRepository.getByLastFourDigits(parsed.lastFourDigits)
            if (card != null) return card

            if (parsed.source == NotificationSource.GOOGLE_WALLET) {
                Log.i(TAG, "No credit card found with last 4 digits: ${parsed.lastFourDigits}, creating placeholder")
                return creditCardRepository.createPlaceholderCard(
                    lastFourDigits = parsed.lastFourDigits,
                    name = "Card ending in ${parsed.lastFourDigits}"
                )
            }
            throw IllegalStateException("No credit card found with last 4 digits: ${parsed.lastFourDigits}")
        }

        val bank = when (parsed.source) {
            NotificationSource.NUBANK -> Bank.NUBANK
            NotificationSource.ITAU -> Bank.ITAU
            else -> throw IllegalArgumentException("Cannot determine bank for source: ${parsed.source}")
        }

        return creditCardRepository.getByBank(bank)
            ?: throw IllegalStateException("No credit card found for bank: ${bank.displayName}")
    }

    companion object {
        private const val TAG = "CreateCreditCardPurchaseUseCase"
    }
}
