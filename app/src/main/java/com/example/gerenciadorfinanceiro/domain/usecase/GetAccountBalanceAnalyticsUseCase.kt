package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAccountBalanceAnalyticsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<AccountBreakdown> {
        return accountRepository.getActiveAccounts().map { accounts ->
            val totalBalance = accounts.sumOf { it.balance }

            val accountAnalytics = accounts.map { account ->
                AccountAnalytics(
                    account = account,
                    balance = account.balance,
                    percentage = if (totalBalance > 0) {
                        (account.balance.toFloat() / totalBalance * 100)
                    } else 0f
                )
            }.sortedByDescending { it.balance }

            AccountBreakdown(
                accounts = accountAnalytics,
                totalBalance = totalBalance
            )
        }
    }
}
