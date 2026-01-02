package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.csv.CsvBillParser
import com.example.gerenciadorfinanceiro.data.csv.CsvFormat
import com.example.gerenciadorfinanceiro.data.csv.CsvParseResult
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

sealed class ImportResult {
    data class Success(val itemCount: Int, val totalAmount: Long) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class ImportCsvBillUseCase @Inject constructor(
    private val csvParser: CsvBillParser,
    private val itemRepository: CreditCardItemRepository,
    private val billRepository: CreditCardBillRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase
) {
    /**
     * Import credit card bill items from a CSV file
     * @param inputStream The CSV file input stream
     * @param creditCardId The credit card to import to
     * @param month Target bill month (1-12)
     * @param year Target bill year
     * @param format The CSV format to use
     * @return ImportResult with success count or error message
     */
    suspend operator fun invoke(
        inputStream: InputStream,
        creditCardId: Long,
        month: Int,
        year: Int,
        format: CsvFormat
    ): ImportResult {
        // Parse the CSV
        val parseResult = csvParser.parse(inputStream, format)

        return when (parseResult) {
            is CsvParseResult.Error -> {
                ImportResult.Error(parseResult.message)
            }
            is CsvParseResult.Success -> {
                if (parseResult.items.isEmpty()) {
                    return ImportResult.Error("Nenhum item encontrado no arquivo CSV")
                }

                try {
                    importItems(parseResult.items, creditCardId, month, year)
                } catch (e: Exception) {
                    ImportResult.Error("Erro ao importar: ${e.message}")
                }
            }
        }
    }

    private suspend fun importItems(
        items: List<CsvBillItem>,
        creditCardId: Long,
        month: Int,
        year: Int
    ): ImportResult {
        // Track all bills that need their totals updated
        val affectedBillIds = mutableSetOf<Long>()
        var totalItemsCreated = 0
        var totalAmountImported = 0L

        // Get or create the bill for the specified month (the "current" bill being imported)
        val currentBill = getOrCreateBillUseCase(creditCardId, month, year)
        affectedBillIds.add(currentBill.id)

        for (csvItem in items) {
            // Generate a unique group ID for installment purchases
            val installmentGroupId = if (csvItem.totalInstallments > 1) {
                UUID.randomUUID().toString()
            } else {
                null
            }

            // Log installment info for debugging
            android.util.Log.d("ImportCSV", "Item: ${csvItem.description}, Installment: ${csvItem.installmentNumber}/${csvItem.totalInstallments}")

            // Create item for the current bill
            val currentItem = CreditCardItem(
                creditCardBillId = currentBill.id,
                category = csvItem.category,
                description = csvItem.description,
                amount = csvItem.amount,
                purchaseDate = csvItem.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                installmentNumber = csvItem.installmentNumber,
                totalInstallments = csvItem.totalInstallments,
                installmentGroupId = installmentGroupId
            )
            itemRepository.insert(currentItem)
            totalItemsCreated++
            totalAmountImported += csvItem.amount

            // If this is an installment purchase with remaining installments, create future items
            if (csvItem.totalInstallments > 1 && csvItem.installmentNumber < csvItem.totalInstallments) {
                val remainingInstallments = csvItem.totalInstallments - csvItem.installmentNumber
                val baseDate = LocalDate.of(year, month, 1)
                android.util.Log.d("ImportCSV", "Creating $remainingInstallments future installments")

                for (i in 1..remainingInstallments) {
                    val futureInstallmentNumber = csvItem.installmentNumber + i
                    val futureDate = baseDate.plusMonths(i.toLong())

                    // Get or create the future bill
                    val futureBill = getOrCreateBillUseCase(
                        creditCardId,
                        futureDate.monthValue,
                        futureDate.year
                    )
                    affectedBillIds.add(futureBill.id)
                    android.util.Log.d("ImportCSV", "Created installment $futureInstallmentNumber in ${futureDate.monthValue}/${futureDate.year}")

                    // Update description to show correct installment number
                    val futureDescription = updateInstallmentDescription(
                        csvItem.description,
                        futureInstallmentNumber,
                        csvItem.totalInstallments
                    )

                    val futureItem = CreditCardItem(
                        creditCardBillId = futureBill.id,
                        category = csvItem.category,
                        description = futureDescription,
                        amount = csvItem.amount, // Same amount for each installment
                        purchaseDate = csvItem.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        installmentNumber = futureInstallmentNumber,
                        totalInstallments = csvItem.totalInstallments,
                        installmentGroupId = installmentGroupId
                    )
                    itemRepository.insert(futureItem)
                    totalItemsCreated++
                }
            }
        }

        // Update totals for all affected bills
        for (billId in affectedBillIds) {
            val newTotal = itemRepository.getTotalAmountByBill(billId)
            billRepository.updateTotalAmount(billId, newTotal)
        }

        return ImportResult.Success(totalItemsCreated, totalAmountImported)
    }

    /**
     * Updates the description to reflect a different installment number
     * E.g., "Purchase 5/10" becomes "Purchase 6/10"
     */
    private fun updateInstallmentDescription(
        originalDescription: String,
        newInstallmentNumber: Int,
        totalInstallments: Int
    ): String {
        // Try to find and replace installment pattern in description
        val patterns = listOf(
            Regex("""(\d+)/(\d+)"""),
            Regex("""parcela\s*(\d+)\s*de\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""parc\.?\s*(\d+)\s*de\s*(\d+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            if (pattern.containsMatchIn(originalDescription)) {
                return originalDescription.replace(pattern) {
                    when {
                        it.value.contains("/") -> "$newInstallmentNumber/$totalInstallments"
                        it.value.lowercase().contains("parcela") -> "parcela $newInstallmentNumber de $totalInstallments"
                        else -> "parc $newInstallmentNumber de $totalInstallments"
                    }
                }
            }
        }

        // If no pattern found, append the installment info
        return "$originalDescription ($newInstallmentNumber/$totalInstallments)"
    }

    /**
     * Parse CSV and return preview of items without importing
     */
    fun parsePreview(inputStream: InputStream, format: CsvFormat): CsvParseResult {
        return csvParser.parse(inputStream, format)
    }

    /**
     * Import pre-parsed items (allows filtering before import)
     * @param items The items to import
     * @param creditCardId The credit card to import to
     * @param month Target bill month (1-12)
     * @param year Target bill year
     * @return ImportResult with success count or error message
     */
    suspend fun importParsedItems(
        items: List<CsvBillItem>,
        creditCardId: Long,
        month: Int,
        year: Int
    ): ImportResult {
        if (items.isEmpty()) {
            return ImportResult.Error("Nenhum item selecionado para importar")
        }

        return try {
            importItems(items, creditCardId, month, year)
        } catch (e: Exception) {
            ImportResult.Error("Erro ao importar: ${e.message}")
        }
    }

    /**
     * Try to auto-detect the CSV format
     */
    fun detectFormat(inputStream: InputStream): CsvFormat? {
        return csvParser.detectFormat(inputStream)
    }
}

