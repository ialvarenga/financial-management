package com.example.gerenciadorfinanceiro.domain.notification

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class ItauNotificationParser @Inject constructor() : NotificationParser {

    private val pixReceivedPattern = Regex("PIX recebido.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val pixSentPattern = Regex("PIX enviado.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val creditCardPurchasePattern = Regex(
        "Compra aprovada de R\\$\\s*([\\d.,]+)\\s+em\\s+(.+?)\\s+no dia",
        RegexOption.IGNORE_CASE
    )

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.ITAU
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        val combined = "$title $text"
        Log.d(TAG, "Parsing Itaú notification: $combined")

        val receivedMatch = pixReceivedPattern.find(combined)
        if (receivedMatch != null) {
            Log.d(TAG, "Matched received pattern: ${receivedMatch.value}")
            val amountStr = "R$ ${receivedMatch.groupValues[1]}"
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.ITAU,
                amount = amount,
                description = "PIX recebido",
                timestamp = timestamp,
                transactionType = TransactionType.INCOME,
                lastFourDigits = null
            )
        }

        val sentMatch = pixSentPattern.find(combined)
        if (sentMatch != null) {
            Log.d(TAG, "Matched sent pattern: ${sentMatch.value}")
            val amountStr = "R$ ${sentMatch.groupValues[1]}"
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.ITAU,
                amount = amount,
                description = "PIX enviado",
                timestamp = timestamp,
                transactionType = TransactionType.EXPENSE,
                lastFourDigits = null
            )
        }

        val creditCardMatch = creditCardPurchasePattern.find(combined)
        if (creditCardMatch != null) {
            Log.d(TAG, "Matched credit card purchase pattern: ${creditCardMatch.value}")
            val amountStr = "R$ ${creditCardMatch.groupValues[1]}"
            val amount = amountStr.toCents() ?: return null
            val place = creditCardMatch.groupValues[2].trim()
            return ParsedNotification(
                source = NotificationSource.ITAU,
                amount = amount,
                description = place,
                timestamp = timestamp,
                transactionType = null,  // Credit card purchase, not a direct transaction
                lastFourDigits = null
            )
        }

        Log.d(TAG, "No pattern matched for Itaú notification")
        return null
    }

    companion object {
        private const val TAG = "ItauNotificationParser"
    }
}
