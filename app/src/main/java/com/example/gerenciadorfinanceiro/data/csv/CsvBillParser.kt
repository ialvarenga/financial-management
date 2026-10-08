package com.example.gerenciadorfinanceiro.data.csv

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

data class SkippedLine(val lineNumber: Int, val content: String, val reason: String)

sealed class CsvParseResult {
    data class Success(
        val items: List<CsvBillItem>,
        val skippedLines: List<SkippedLine> = emptyList()
    ) : CsvParseResult()
    data class Error(val message: String, val line: Int? = null) : CsvParseResult()
}

@Singleton
class CsvBillParser @Inject constructor() {

    companion object {
        private const val TAG = "CsvBillParser"

        // Generous upper bound for a credit card installment plan (5 years of monthly bills).
        private const val MAX_INSTALLMENTS = 60

        // .xlsx files are ZIP archives; legacy .xls files are OLE2 compound documents.
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        private val OLE2_MAGIC = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte())

        // Day zero of Excel's date serial numbers. The 1900 system starts at 1899-12-30
        // (not 12-31) because Excel counts a nonexistent 1900-02-29.
        private val EXCEL_EPOCH_1900 = LocalDate.of(1899, 12, 30)
        private val EXCEL_EPOCH_1904 = LocalDate.of(1904, 1, 1)
    }

    /**
     * Parse a CSV or .xlsx file from an InputStream using the user-configured columns/formats.
     * The file type is detected from its content. For .xlsx, the first sheet is read and the
     * delimiter is ignored; date and decimal formats only apply to cells stored as text.
     * Tolerant: bad lines are collected as SkippedLine instead of aborting the import.
     * @param inputStream The input stream of the CSV or .xlsx file
     * @param config Column indices, delimiter, date pattern and decimal style
     * @return CsvParseResult containing parsed items or an error
     */
    fun parse(inputStream: InputStream, config: CsvImportConfig = CsvImportConfig()): CsvParseResult {
        return try {
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.isEmpty()) {
                Log.w(TAG, "parse: file is empty")
                return CsvParseResult.Error("Arquivo vazio")
            }

            when {
                bytes.startsWith(ZIP_MAGIC) -> {
                    val sheet = XlsxReader.read(bytes)
                    Log.d(TAG, "parse: read ${sheet.rows.size} spreadsheet rows")
                    parseRows(sheet.rows, config, if (sheet.date1904) EXCEL_EPOCH_1904 else EXCEL_EPOCH_1900)
                }
                bytes.startsWith(OLE2_MAGIC) -> {
                    Log.w(TAG, "parse: legacy .xls file")
                    CsvParseResult.Error("Arquivos .xls não são suportados. Salve a planilha como .xlsx ou CSV")
                }
                else -> {
                    val lines = decodeToLines(bytes)
                    Log.d(TAG, "parse: read ${lines.size} lines")

                    if (lines.isEmpty()) {
                        Log.w(TAG, "parse: file is empty")
                        return CsvParseResult.Error("Arquivo vazio")
                    }

                    val rows = lines.mapIndexed { index, line ->
                        SheetRow(index + 1, parseCsvLine(line, config.delimiter.char).map { SheetCell.Text(it) }, line)
                    }
                    parseRows(rows, config, EXCEL_EPOCH_1900)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse: failed to read file", e)
            CsvParseResult.Error("Erro ao ler arquivo: ${e.message}")
        }
    }

    /**
     * Decodes raw file bytes into lines, honoring a UTF-8/UTF-16 BOM when present.
     * Falls back to Windows-1252 (a superset of ISO-8859-1 covering the accented
     * characters used by pt-BR bank exports) when the bytes aren't valid UTF-8.
     */
    private fun decodeToLines(bytes: ByteArray): List<String> {
        val text = decodeText(bytes)
        val lines = text.lines()
        return if (lines.isNotEmpty() && lines.last().isEmpty()) lines.dropLast(1) else lines
    }

    private fun decodeText(bytes: ByteArray): String {
        val bomUtf8 = bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        if (bomUtf8) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        val bomUtf16Le = bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()
        if (bomUtf16Le) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        val bomUtf16Be = bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()
        if (bomUtf16Be) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            Log.w(TAG, "parse: content is not valid UTF-8, falling back to Windows-1252")
            String(bytes, Charset.forName("windows-1252"))
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    // Custom format: user-defined columns, date/decimal format and skip rows.
    // Tolerant: bad lines are collected as SkippedLine instead of aborting the import.
    private fun parseRows(rows: List<SheetRow>, config: CsvImportConfig, excelEpoch: LocalDate): CsvParseResult {
        val dateFormatter = try {
            DateTimeFormatter.ofPattern(config.datePattern)
        } catch (e: IllegalArgumentException) {
            return CsvParseResult.Error("Formato de data inválido: ${config.datePattern}")
        }

        val items = mutableListOf<CsvBillItem>()
        val skipped = mutableListOf<SkippedLine>()
        val maxColumn = maxOf(config.dateColumn, config.descriptionColumn, config.amountColumn)
        // Filtering by row number (instead of dropping N rows) matches the row numbers the user
        // sees in a spreadsheet, where empty rows aren't stored in the file.
        val dataRows = rows.filter { it.rowNumber > config.skipRows }
        Log.d(TAG, "parseRows: ${dataRows.size} data rows, config=$config")

        for (row in dataRows) {
            val line = row.content
            val lineNumber = row.rowNumber
            if (row.cells.all { it == null || (it is SheetCell.Text && it.value.isBlank()) }) continue

            try {
                if (row.cells.size < maxColumn) {
                    skipped.add(SkippedLine(lineNumber, line, "Colunas insuficientes (${row.cells.size})"))
                    continue
                }

                val date = parseDate(row.cellAt(config.dateColumn), dateFormatter, excelEpoch)
                val description = when (val cell = row.cellAt(config.descriptionColumn)) {
                    is SheetCell.Text -> cell.value.trim()
                    is SheetCell.Number -> cell.value.toPlainString()
                }
                val amount = when (val cell = row.cellAt(config.amountColumn)) {
                    is SheetCell.Text -> parseAmountCustom(cell.value, config.decimalStyle)
                    is SheetCell.Number -> cell.value.toCents()
                }

                if (amount < 0 && config.negativeHandling == NegativeHandling.SKIP) {
                    skipped.add(SkippedLine(lineNumber, line, "Valor negativo ignorado"))
                    continue
                }

                val (installmentNumber, totalInstallments) = parseInstallments(description)
                items.add(
                    CsvBillItem(
                        date = date,
                        description = description,
                        amount = amount,
                        category = detectCategory(description),
                        installmentNumber = installmentNumber,
                        totalInstallments = totalInstallments
                    )
                )
            } catch (e: DateTimeParseException) {
                skipped.add(SkippedLine(lineNumber, line, "Data inválida"))
            } catch (e: Exception) {
                Log.w(TAG, "parseRows: error on line $lineNumber: $line", e)
                skipped.add(SkippedLine(lineNumber, line, e.message ?: "Erro ao interpretar linha"))
            }
        }

        Log.d(TAG, "parseRows: parsed ${items.size} items, skipped ${skipped.size} lines")

        if (items.isEmpty()) {
            val firstReason = skipped.firstOrNull()?.reason ?: "arquivo vazio"
            return CsvParseResult.Error(
                "Nenhuma linha pôde ser interpretada. Verifique as colunas e os formatos. Primeiro erro: $firstReason"
            )
        }

        return CsvParseResult.Success(items, skipped)
    }

    /** 1-based column; a missing cell reads as empty text so it fails like an empty CSV field. */
    private fun SheetRow.cellAt(column: Int): SheetCell = cells[column - 1] ?: SheetCell.Text("")

    private fun parseDate(cell: SheetCell, formatter: DateTimeFormatter, excelEpoch: LocalDate): LocalDate =
        when (cell) {
            is SheetCell.Text -> LocalDate.parse(cell.value.trim(), formatter)
            // Excel date serial: days since the epoch, with the time of day as the fraction
            is SheetCell.Number -> excelEpoch.plusDays(cell.value.setScale(0, RoundingMode.FLOOR).toLong())
        }

    private fun BigDecimal.toCents(): Long =
        setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).longValueExact()

    private fun parseAmountCustom(raw: String, style: DecimalStyle): Long {
        val cleaned = raw
            .replace("R$", "")
            .replace("$", "")
            .replace("\"", "")
            .trim()
        val normalized = when (style) {
            DecimalStyle.BRAZILIAN -> cleaned.replace(".", "").replace(",", ".")
            DecimalStyle.US -> cleaned.replace(",", "")
        }

        val value = try {
            BigDecimal(normalized)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Valor inválido: $raw")
        }

        return value.toCents()
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }

    private fun parseInstallments(description: String): Pair<Int, Int> {
        // Common patterns: "2/6", "parcela 2 de 6", "2 de 6", "parc 2/6"
        val patterns = listOf(
            Regex("""(\d+)/(\d+)"""),
            Regex("""parcela\s*(\d+)\s*de\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""parc\.?\s*(\d+)\s*de\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*de\s*(\d+)""")
        )

        for (pattern in patterns) {
            pattern.find(description)?.let { match ->
                val current = match.groupValues[1].toIntOrNull() ?: 1
                val total = match.groupValues[2].toIntOrNull() ?: 1
                // Real installment plans don't run past a few years of monthly bills.
                // Without this cap, unrelated numeric pairs in the description (e.g.
                // "Protocolo 45/9876") get misread as installment 45 of 9876.
                if (total in 2..MAX_INSTALLMENTS && current in 1..total) {
                    return current to total
                }
            }
        }

        return 1 to 1
    }

    private fun detectCategory(description: String): Category {
        val lowerDesc = description.lowercase()

        return when {
            // Food
            lowerDesc.containsAny(listOf("ifood", "uber eats", "rappi", "restaurante", "lanchonete",
                "pizzaria", "padaria", "mercado", "supermercado", "hortifruti", "açougue")) -> Category.FOOD

            // Transport
            lowerDesc.containsAny(listOf("uber", "99", "cabify", "posto", "combustivel", "combustível",
                "estacionamento", "parking", "pedágio", "pedagio")) -> Category.TRANSPORT

            // Health
            lowerDesc.containsAny(listOf("farmácia", "farmacia", "drogaria", "hospital", "clínica",
                "clinica", "médico", "medico", "dentista", "laboratorio", "laboratório")) -> Category.HEALTH

            // Entertainment
            lowerDesc.containsAny(listOf("netflix", "spotify", "prime video", "hbo", "disney",
                "cinema", "teatro", "show", "ingresso", "game", "steam", "playstation", "xbox")) -> Category.ENTERTAINMENT

            // Shopping
            lowerDesc.containsAny(listOf("amazon", "mercado livre", "magazine", "americanas",
                "shopee", "aliexpress", "shein", "loja", "store")) -> Category.SHOPPING

            // Services
            lowerDesc.containsAny(listOf("serviço", "servico", "manutenção", "manutencao",
                "conserto", "reparo")) -> Category.SERVICES

            // Subscriptions
            lowerDesc.containsAny(listOf("assinatura", "mensalidade", "subscription", "plano")) -> Category.SUBSCRIPTIONS

            // Bills
            lowerDesc.containsAny(listOf("energia", "água", "agua", "gás", "gas", "internet",
                "telefone", "celular", "condominio", "condomínio", "aluguel")) -> Category.BILLS

            // Education
            lowerDesc.containsAny(listOf("curso", "escola", "faculdade", "universidade",
                "livro", "apostila", "udemy", "coursera", "alura")) -> Category.EDUCATION

            // Clothing
            lowerDesc.containsAny(listOf("roupa", "calçado", "calcado", "sapato", "tênis",
                "tenis", "camisa", "calça", "calca", "vestido", "renner", "c&a", "riachuelo")) -> Category.CLOTHING

            // Personal Care
            lowerDesc.containsAny(listOf("salão", "salao", "barbearia", "cabelo", "unha",
                "manicure", "estética", "estetica", "cosmético", "cosmetico")) -> Category.PERSONAL_CARE

            // Travel
            lowerDesc.containsAny(listOf("passagem", "aéreo", "aereo", "hotel", "hospedagem",
                "airbnb", "booking", "latam", "gol", "azul")) -> Category.TRAVEL

            // Pets
            lowerDesc.containsAny(listOf("pet", "ração", "racao", "veterinário", "veterinario",
                "petshop", "pet shop")) -> Category.PETS

            else -> Category.OTHER
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { this.contains(it) }
    }
}
