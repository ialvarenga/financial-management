package com.example.gerenciadorfinanceiro.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.backup.ExportResult
import com.example.gerenciadorfinanceiro.data.backup.ImportResult
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
    val exportSuccess: String? = null,
    val importSuccess: ImportSuccessInfo? = null,
    val errorMessage: String? = null
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
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

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

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importSuccess = null, errorMessage = null) }

            when (val result = importBackupUseCase.execute(uri)) {
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

    fun clearMessages() {
        _uiState.update {
            it.copy(
                exportSuccess = null,
                importSuccess = null,
                errorMessage = null
            )
        }
    }
}
