package com.example.gerenciadorfinanceiro.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _accounts = accountRepository.getActiveAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _totalBalance = accountRepository.getTotalBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val uiState: StateFlow<AccountsUiState> = combine(
        _accounts,
        _totalBalance
    ) { accounts, totalBalance ->
        AccountsUiState(
            accounts = accounts,
            totalBalance = totalBalance,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.delete(account)
        }
    }
}
