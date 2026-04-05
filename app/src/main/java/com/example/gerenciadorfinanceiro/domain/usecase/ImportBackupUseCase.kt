package com.example.gerenciadorfinanceiro.domain.usecase

import android.net.Uri
import com.example.gerenciadorfinanceiro.data.backup.BackupData
import com.example.gerenciadorfinanceiro.data.backup.BackupFileService
import com.example.gerenciadorfinanceiro.data.backup.FinancialData
import com.example.gerenciadorfinanceiro.data.backup.ImportEntityFilter
import com.example.gerenciadorfinanceiro.data.backup.ImportResult
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupFileService: BackupFileService
) {
    suspend fun readBackup(uri: Uri): Result<BackupData> {
        return backupFileService.importFromFile(uri)
    }

    suspend fun execute(uri: Uri): ImportResult {
        return executeInternal(uri, ImportEntityFilter())
    }

    suspend fun executeWithData(data: FinancialData, filter: ImportEntityFilter): ImportResult {
        return try {
            backupRepository.importAllData(data, filter)

            ImportResult.Success(
                accountCount = if (filter.accounts) data.accounts.size else 0,
                transactionCount = if (filter.transactions) data.transactions.size else 0,
                creditCardCount = if (filter.creditCards) data.creditCards.size else 0,
                recurrenceCount = if (filter.recurrences) data.recurrences.size else 0,
                transferCount = if (filter.transfers) data.transfers.size else 0,
                creditCardBillCount = if (filter.creditCardBills) data.creditCardBills.size else 0,
                creditCardItemCount = if (filter.creditCardItems) data.creditCardItems.size else 0
            )
        } catch (e: IllegalStateException) {
            ImportResult.Error("Dados inconsistentes no backup: ${e.message}")
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Erro inesperado ao importar dados")
        }
    }

    private suspend fun executeInternal(uri: Uri, filter: ImportEntityFilter): ImportResult {
        return try {
            val backupData = backupFileService.importFromFile(uri)
                .getOrElse { exception ->
                    return ImportResult.Error(exception.message ?: "Erro ao ler arquivo de backup")
                }

            if (backupData.version != 1) {
                return ImportResult.Error("Versão do backup não compatível (versão ${backupData.version})")
            }

            executeWithData(backupData.data, filter)
        } catch (e: IllegalStateException) {
            ImportResult.Error("Dados inconsistentes no backup: ${e.message}")
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Erro inesperado ao importar dados")
        }
    }
}
