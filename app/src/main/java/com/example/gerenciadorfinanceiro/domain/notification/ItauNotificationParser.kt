package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class ItauNotificationParser @Inject constructor() : NotificationParser {

    private val pixReceivedPattern = Regex("PIX recebido.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val pixSentPattern = Regex("PIX enviado.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.ITAU
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        val combined = "$title $text"

        val receivedMatch = pixReceivedPattern.find(combined)
        if (receivedMatch != null) {
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

        return null
    }
}
