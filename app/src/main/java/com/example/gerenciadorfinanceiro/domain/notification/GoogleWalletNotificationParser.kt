package com.example.gerenciadorfinanceiro.domain.notification

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class GoogleWalletNotificationParser @Inject constructor() : NotificationParser {

    private val amountPattern = Regex("R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val cardEndingPattern = Regex(
        "(?:termina(?:\\s+em)?|final(?:\\s+do)?|••••|\\*{4})\\s*(\\d{4})",
        RegexOption.IGNORE_CASE
    )

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.GOOGLE_WALLET
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        Log.d(TAG, "Parsing Google Wallet notification - Title: $title, Text: $text")

        val amountMatch = amountPattern.find(text)
        val cardEndingMatch = cardEndingPattern.find(text)
        if (amountMatch == null || cardEndingMatch == null) {
            Log.d(TAG, "No pattern matched for Google Wallet notification")
            return null
        }

        Log.d(TAG, "Matched purchase pattern")

        val amountStr = "R$ ${amountMatch.groupValues[1]}"
        val amount = amountStr.toCents() ?: return null
        val lastFour = cardEndingMatch.groupValues[1]

        // Use title as description (contains the place name)
        val description = title.ifBlank { "Compra Google Wallet" }

        return ParsedNotification(
            source = NotificationSource.GOOGLE_WALLET,
            amount = amount,
            description = description,
            timestamp = timestamp,
            transactionType = null,  // Credit card purchase, not a direct transaction
            lastFourDigits = lastFour,
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
    }

    companion object {
        private const val TAG = "GoogleWalletParser"
    }
}
