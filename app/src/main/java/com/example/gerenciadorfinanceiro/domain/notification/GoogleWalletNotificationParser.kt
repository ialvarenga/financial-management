package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class GoogleWalletNotificationParser @Inject constructor() : NotificationParser {

    private val purchasePattern = Regex(
        ".*R\\$\\s*([\\d.,]+).*[•*]{4}\\s*(\\d{4})",
        RegexOption.IGNORE_CASE
    )

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.GOOGLE_WALLET
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        val combined = "$title $text"

        val match = purchasePattern.find(combined) ?: return null

        val amountStr = "R$ ${match.groupValues[1]}"
        val amount = amountStr.toCents() ?: return null
        val lastFour = match.groupValues[2]

        return ParsedNotification(
            source = NotificationSource.GOOGLE_WALLET,
            amount = amount,
            description = "Compra Google Wallet",
            timestamp = timestamp,
            transactionType = TransactionType.EXPENSE,
            lastFourDigits = lastFour
        )
    }
}
