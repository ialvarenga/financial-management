package com.example.gerenciadorfinanceiro.domain.notification

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class GoogleWalletNotificationParser @Inject constructor() : NotificationParser {

    private val purchasePattern = Regex(
        "R\\$\\s*([\\d.,]+).*?(\\d{4})$",
        RegexOption.IGNORE_CASE
    )

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.GOOGLE_WALLET
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        Log.d(TAG, "Parsing Google Wallet notification - Title: $title, Text: $text")

        val match = purchasePattern.find(text)
        if (match == null) {
            Log.d(TAG, "No pattern matched for Google Wallet notification")
            return null
        }

        Log.d(TAG, "Matched purchase pattern: ${match.value}")

        val amountStr = "R$ ${match.groupValues[1]}"
        val amount = amountStr.toCents() ?: return null
        val lastFour = match.groupValues[2]

        // Use title as description (contains the place name)
        val description = title.ifBlank { "Compra Google Wallet" }

        return ParsedNotification(
            source = NotificationSource.GOOGLE_WALLET,
            amount = amount,
            description = description,
            timestamp = timestamp,
            transactionType = null,  // Credit card purchase, not a direct transaction
            lastFourDigits = lastFour
        )
    }

    companion object {
        private const val TAG = "GoogleWalletParser"
    }
}
