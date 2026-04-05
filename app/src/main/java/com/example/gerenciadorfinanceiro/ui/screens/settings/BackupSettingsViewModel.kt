package com.example.gerenciadorfinanceiro.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.backup.BackupPreviewInfo
import com.example.gerenciadorfinanceiro.data.backup.ExportResult
import com.example.gerenciadorfinanceiro.data.backup.FinancialData
import com.example.gerenciadorfinanceiro.data.backup.ImportEntity
import com.example.gerenciadorfinanceiro.data.backup.ImportEntityFilter
import com.example.gerenciadorfinanceiro.data.backup.ImportResult
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import com.example.gerenciadorfinanceiro.domain.usecase.ExportBackupUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupSettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isLoadingBackup: Boolean = false,
    val isResetting: Boolean = false,
    val exportSuccess: String? = null,
    val importSuccess: ImportSuccessInfo? = null,
    val resetSuccess: Boolean = false,
    val errorMessage: String? = null,
    val backupPreview: BackupPreviewInfo? = null,
    val entityFilter: ImportEntityFilter = ImportEntityFilter()
)

data class ImportSuccessInfo(
    val accountCount: Int,
    val transactionCount: Int,
    val creditCardCount: Int,
    val recurrenceCount: Int,
    val transferCount: Int,
    val creditCardBillCount: Int,
    val creditCardItemCount: Int
)

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

    private var pendingBackupData: FinancialData? = null

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportSuccess = null, errorMessage = null) }

            when (val result = exportBackupUseCase.execute(uri)) {
                is ExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = result.fileName
                        )
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadBackupForPreview(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackup = true, errorMessage = null) }
            val result = importBackupUseCase.readBackup(uri)
            result.fold(
                onSuccess = { backupData ->
                    if (backupData.version != 1) {
                        _uiState.update {
                            it.copy(
                                isLoadingBackup = false,
                                errorMessage = "Versão do backup não compatível (versão ${backupData.version})"
                            )
                        }
                        return@launch
                    }
                    pendingBackupData = backupData.data
                    _uiState.update {
                        it.copy(
                            isLoadingBackup = false,
                            backupPreview = BackupPreviewInfo(
                                accountCount = backupData.data.accounts.size,
                                transactionCount = backupData.data.transactions.size,
                                creditCardCount = backupData.data.creditCards.size,
                                recurrenceCount = backupData.data.recurrences.size,
                                transferCount = backupData.data.transfers.size,
                                creditCardBillCount = backupData.data.creditCardBills.size,
                                creditCardItemCount = backupData.data.creditCardItems.size
                            ),
                            entityFilter = ImportEntityFilter()
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingBackup = false,
                            errorMessage = exception.message ?: "Erro ao ler arquivo de backup"
                        )
                    }
                }
            )
        }
    }

    fun toggleEntityFilter(entity: ImportEntity, checked: Boolean) {
        val current = _uiState.value.entityFilter
        val updated = when (entity) {
            ImportEntity.ACCOUNTS -> {
                if (!checked) {
                    current.copy(accounts = false, transactions = false, transfers = false)
                } else {
                    current.copy(accounts = true)
                }
            }
            ImportEntity.CREDIT_CARDS -> {
                if (!checked) {
                    current.copy(creditCards = false, creditCardBills = false, creditCardItems = false)
                } else {
                    current.copy(creditCards = true)
                }
            }
            ImportEntity.TRANSACTIONS -> current.copy(transactions = checked)
            ImportEntity.RECURRENCES -> current.copy(recurrences = checked)
            ImportEntity.TRANSFERS -> current.copy(transfers = checked)
            ImportEntity.CREDIT_CARD_BILLS -> {
                if (!checked) {
                    current.copy(creditCardBills = false, creditCardItems = false)
                } else {
                    current.copy(creditCardBills = true)
                }
            }
            ImportEntity.CREDIT_CARD_ITEMS -> current.copy(creditCardItems = checked)
        }
        _uiState.update { it.copy(entityFilter = updated) }
    }

    fun confirmImport() {
        val data = pendingBackupData ?: return
        val filter = _uiState.value.entityFilter
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, backupPreview = null, errorMessage = null) }
            pendingBackupData = null

            when (val result = importBackupUseCase.executeWithData(data, filter)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = ImportSuccessInfo(
                                accountCount = result.accountCount,
                                transactionCount = result.transactionCount,
                                creditCardCount = result.creditCardCount,
                                recurrenceCount = result.recurrenceCount,
                                transferCount = result.transferCount,
                                creditCardBillCount = result.creditCardBillCount,
                                creditCardItemCount = result.creditCardItemCount
                            )
                        )
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun cancelImport() {
        pendingBackupData = null
        _uiState.update { it.copy(backupPreview = null) }
    }

    // Keep legacy import for backward compatibility
    fun importBackup(uri: Uri) {
        loadBackupForPreview(uri)
    }

    fun resetAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true, resetSuccess = false, errorMessage = null) }
            try {
                backupRepository.resetAllData()
                _uiState.update { it.copy(isResetting = false, resetSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isResetting = false,
                        errorMessage = e.message ?: "Erro ao resetar dados"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                exportSuccess = null,
                importSuccess = null,
                resetSuccess = false,
                errorMessage = null
            )
        }
    }
}
