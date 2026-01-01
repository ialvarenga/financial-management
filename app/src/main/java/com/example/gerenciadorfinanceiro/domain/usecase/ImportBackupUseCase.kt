package com.example.gerenciadorfinanceiro.domain.usecase

import android.net.Uri
import com.example.gerenciadorfinanceiro.data.backup.BackupFileService
import com.example.gerenciadorfinanceiro.data.backup.ImportResult
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupFileService: BackupFileService
) {
    suspend fun execute(uri: Uri): ImportResult {
        return try {
            val backupData = backupFileService.importFromFile(uri)
                .getOrElse { exception ->
                    return ImportResult.Error(exception.message ?: "Erro ao ler arquivo de backup")
                }

            if (backupData.version != 1) {
                return ImportResult.Error("Versão do backup não compatível (versão ${backupData.version})")
            }

            backupRepository.importAllData(backupData.data)

            ImportResult.Success(
                accountCount = backupData.data.accounts.size,
                transactionCount = backupData.data.transactions.size,
                creditCardCount = backupData.data.creditCards.size,
                recurrenceCount = backupData.data.recurrences.size,
                transferCount = backupData.data.transfers.size,
                creditCardBillCount = backupData.data.creditCardBills.size,
                creditCardItemCount = backupData.data.creditCardItems.size
            )
        } catch (e: IllegalStateException) {
            ImportResult.Error("Dados inconsistentes no backup: ${e.message}")
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Erro inesperado ao importar dados")
        }
    }
}
