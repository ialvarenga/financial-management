package com.example.gerenciadorfinanceiro.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gerenciadorfinanceiro.data.backup.BackupPreviewInfo
import com.example.gerenciadorfinanceiro.data.backup.ExportResult
import com.example.gerenciadorfinanceiro.data.backup.FinancialData
import com.example.gerenciadorfinanceiro.data.backup.ImportEntity
import com.example.gerenciadorfinanceiro.data.backup.ImportEntityFilter
import com.example.gerenciadorfinanceiro.data.backup.ImportResult
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.usecase.ExportBackupUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ImportBackupUseCase
import com.example.gerenciadorfinanceiro.worker.AutoBackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

data class AutoBackupUiState(
    val enabled: Boolean = true,
    val folderUri: String? = null,
    val lastBackupAt: Long? = null
)

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

    val autoBackupState: StateFlow<AutoBackupUiState> = combine(
        settingsRepository.isAutoBackupEnabled(),
        settingsRepository.getBackupFolderUri(),
        settingsRepository.getLastAutoBackupAt()
    ) { enabled, folderUri, lastBackupAt ->
        AutoBackupUiState(enabled = enabled, folderUri = folderUri, lastBackupAt = lastBackupAt)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutoBackupUiState()
    )

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(enabled)
        }
    }

    fun setBackupFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.setBackupFolderUri(uri)
        }
    }

    fun backupNow() {
        workManager.enqueueUniqueWork(
            AutoBackupWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setInputData(workDataOf(AutoBackupWorker.KEY_FORCE to true))
                .build()
        )
    }

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
                    // schemaVersion is 0 for backups made before the field existed, which
                    // predates any breaking schema change - those are still safe to restore.
                    // A schemaVersion newer than this app's DB means the backup carries
                    // fields/entities this version doesn't know how to restore correctly.
                    if (backupData.schemaVersion > AppDatabase.DATABASE_VERSION) {
                        _uiState.update {
                            it.copy(
                                isLoadingBackup = false,
                                errorMessage = "Este backup foi criado por uma versão mais recente do app " +
                                    "(schema ${backupData.schemaVersion}, atual ${AppDatabase.DATABASE_VERSION}). " +
                                    "Atualize o app antes de restaurar."
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

    // Accounts/CreditCards/CreditCardBills cascade-delete their dependents at the DB level,
    // so restoring one of them always requires restoring what cascades from it too - keeps
    // this in sync with BackupRepository.validatePartialFilterConsistency.
    fun toggleEntityFilter(entity: ImportEntity, checked: Boolean) {
        val current = _uiState.value.entityFilter
        val updated = when (entity) {
            ImportEntity.ACCOUNTS -> {
                if (!checked) {
                    current.copy(accounts = false, transactions = false, transfers = false)
                } else {
                    current.copy(accounts = true, transactions = true, transfers = true, recurrences = true)
                }
            }
            ImportEntity.CREDIT_CARDS -> {
                if (!checked) {
                    current.copy(creditCards = false, creditCardBills = false, creditCardItems = false)
                } else {
                    current.copy(creditCards = true, creditCardBills = true, creditCardItems = true, recurrences = true)
                }
            }
            ImportEntity.TRANSACTIONS -> {
                if (!checked && current.accounts) current else current.copy(transactions = checked)
            }
            ImportEntity.RECURRENCES -> {
                if (!checked && (current.accounts || current.creditCards)) {
                    // Can't drop recurrences while a parent that cascades into it is selected
                    current
                } else {
                    current.copy(recurrences = checked)
                }
            }
            ImportEntity.TRANSFERS -> {
                if (!checked && current.accounts) current else current.copy(transfers = checked)
            }
            ImportEntity.CREDIT_CARD_BILLS -> {
                if (!checked) {
                    if (current.creditCards) current else current.copy(creditCardBills = false, creditCardItems = false)
                } else {
                    current.copy(creditCardBills = true, creditCardItems = true)
                }
            }
            ImportEntity.CREDIT_CARD_ITEMS -> {
                if (!checked && current.creditCardBills) current else current.copy(creditCardItems = checked)
            }
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
