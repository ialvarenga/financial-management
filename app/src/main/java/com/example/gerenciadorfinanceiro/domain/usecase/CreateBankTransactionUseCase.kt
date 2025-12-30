package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.domain.model.Bank
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import javax.inject.Inject

class CreateBankTransactionUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val createTransactionUseCase: CreateTransactionUseCase
) {
    suspend operator fun invoke(parsed: ParsedNotification, notificationKey: String): ProcessedNotification {
        val bank = when (parsed.source) {
            NotificationSource.ITAU -> Bank.ITAU
            NotificationSource.NUBANK -> Bank.NUBANK
            else -> throw IllegalArgumentException("Invalid source for bank transaction: ${parsed.source}")
        }

        val account = accountRepository.getFirstByBank(bank)
            ?: throw IllegalStateException("No active account found for ${bank.displayName}")

        val transaction = Transaction(
            accountId = account.id,
            amount = parsed.amount,
            type = parsed.transactionType!!,
            category = Category.OTHER,
            paymentMethod = PaymentMethod.PIX,
            status = TransactionStatus.PENDING,
            description = parsed.description,
            date = parsed.timestamp,
            notes = "Auto-created from notification"
        )

        val transactionId = createTransactionUseCase(transaction)

        return ProcessedNotification(
            notificationKey = notificationKey,
            source = parsed.source,
            notificationText = parsed.description,
            createdTransactionId = transactionId
        )
    }
}
