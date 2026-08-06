package com.example.gerenciadorfinanceiro.data.csv

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
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
    }

    /**
     * Parse a CSV file from an InputStream using the user-configured column/delimiter/format.
     * Tolerant: bad lines are collected as SkippedLine instead of aborting the import.
     * @param inputStream The input stream of the CSV file
     * @param config Column indices, delimiter, date pattern and decimal style
     * @return CsvParseResult containing parsed items or an error
     */
    fun parse(inputStream: InputStream, config: CsvImportConfig = CsvImportConfig()): CsvParseResult {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            reader.close()

            Log.d(TAG, "parse: read ${lines.size} lines")

            if (lines.isEmpty()) {
                Log.w(TAG, "parse: file is empty")
                return CsvParseResult.Error("Arquivo CSV vazio")
            }

            parseCustom(lines, config)
        } catch (e: Exception) {
            Log.e(TAG, "parse: failed to read file", e)
            CsvParseResult.Error("Erro ao ler arquivo: ${e.message}")
        }
    }

    // Custom format: user-defined columns, delimiter, date/decimal format and skip rows.
    // Tolerant: bad lines are collected as SkippedLine instead of aborting the import.
    private fun parseCustom(lines: List<String>, config: CsvImportConfig): CsvParseResult {
        val dateFormatter = try {
            DateTimeFormatter.ofPattern(config.datePattern)
        } catch (e: IllegalArgumentException) {
            return CsvParseResult.Error("Formato de data inválido: ${config.datePattern}")
        }

        val items = mutableListOf<CsvBillItem>()
        val skipped = mutableListOf<SkippedLine>()
        val maxColumn = maxOf(config.dateColumn, config.descriptionColumn, config.amountColumn)
        val dataLines = lines.drop(config.skipRows)
        Log.d(TAG, "parseCustom: ${dataLines.size} data lines, config=$config")

        for ((index, line) in dataLines.withIndex()) {
            if (line.isBlank()) continue
            val lineNumber = index + config.skipRows + 1

            try {
                val parts = parseCsvLine(line, config.delimiter.char)
                if (parts.size < maxColumn) {
                    skipped.add(SkippedLine(lineNumber, line, "Colunas insuficientes (${parts.size})"))
                    continue
                }

                val date = LocalDate.parse(parts[config.dateColumn - 1].trim(), dateFormatter)
                val description = parts[config.descriptionColumn - 1].trim()
                val amount = parseAmountCustom(parts[config.amountColumn - 1], config.decimalStyle)

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
                Log.w(TAG, "parseCustom: error on line $lineNumber: $line", e)
                skipped.add(SkippedLine(lineNumber, line, e.message ?: "Erro ao interpretar linha"))
            }
        }

        Log.d(TAG, "parseCustom: parsed ${items.size} items, skipped ${skipped.size} lines")

        if (items.isEmpty()) {
            val firstReason = skipped.firstOrNull()?.reason ?: "arquivo vazio"
            return CsvParseResult.Error(
                "Nenhuma linha pôde ser interpretada. Verifique as colunas e o delimitador. Primeiro erro: $firstReason"
            )
        }

        return CsvParseResult.Success(items, skipped)
    }

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

        return value.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).longValueExact()
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
                if (total > 1 && current <= total) {
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
