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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supported CSV formats for different banks/credit cards
 */
enum class CsvFormat(val displayName: String) {
    NUBANK("Nubank"),
    INTER("Inter"),
    C6BANK("C6 Bank"),
    ITAU("Itaú"),
    BRADESCO("Bradesco"),
    SANTANDER("Santander"),
    GENERIC("Genérico (Data;Descrição;Valor)")
}

sealed class CsvParseResult {
    data class Success(val items: List<CsvBillItem>) : CsvParseResult()
    data class Error(val message: String, val line: Int? = null) : CsvParseResult()
}

@Singleton
class CsvBillParser @Inject constructor() {

    companion object {
        private const val TAG = "CsvBillParser"
    }

    private val brazilianDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Parse a CSV file from an InputStream
     * @param inputStream The input stream of the CSV file
     * @param format The CSV format to use for parsing
     * @return CsvParseResult containing parsed items or an error
     */
    fun parse(inputStream: InputStream, format: CsvFormat): CsvParseResult {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            reader.close()

            Log.d(TAG, "parse: read ${lines.size} lines, format=$format")

            if (lines.isEmpty()) {
                Log.w(TAG, "parse: file is empty")
                return CsvParseResult.Error("Arquivo CSV vazio")
            }

            when (format) {
                CsvFormat.NUBANK -> parseNubank(lines)
                CsvFormat.ITAU -> parseItau(lines)
                else -> parseGeneric(lines)
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse: failed to read file", e)
            CsvParseResult.Error("Erro ao ler arquivo: ${e.message}")
        }
    }

    /**
     * Try to auto-detect the CSV format based on header/content
     */
    fun detectFormat(inputStream: InputStream): CsvFormat? {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val firstLines = (1..5).mapNotNull { reader.readLine() }
            reader.close()

            val content = firstLines.joinToString("\n").lowercase()

            val detected = when {
                content.contains("nubank") || content.contains("date,title,amount") -> CsvFormat.NUBANK
                content.contains("inter") || content.contains("data;lancamento;valor") -> CsvFormat.INTER
                content.contains("c6") || content.contains("data da compra") -> CsvFormat.C6BANK
                content.contains("itau") || content.contains("itaú") -> CsvFormat.ITAU
                content.contains("bradesco") -> CsvFormat.BRADESCO
                content.contains("santander") -> CsvFormat.SANTANDER
                else -> CsvFormat.GENERIC
            }

            Log.d(TAG, "detectFormat: detected=$detected")
            detected
        } catch (e: Exception) {
            Log.e(TAG, "detectFormat: failed to detect format", e)
            null
        }
    }

    // Nubank format: date,title,amount (uses commas, amount in negative for expenses)
    private fun parseNubank(lines: List<String>): CsvParseResult {
        val items = mutableListOf<CsvBillItem>()
        val dataLines = skipHeader(lines, listOf("date", "title", "amount"))
        Log.d(TAG, "parseNubank: ${dataLines.size} data lines")

        for ((index, line) in dataLines.withIndex()) {
            if (line.isBlank()) continue

            try {
                val parts = parse3FieldCsvLine(line, ',')
                if (parts.size < 3) {
                    Log.w(TAG, "parseNubank: skipping line ${index + 2}, not enough fields (${parts.size}): $line")
                    continue
                }

                val date = parseDate(parts[0].trim())
                val description = parts[1].trim()
                val amountStr = parts[2].trim().replace(",", ".").replace("\"", "")
                val amount = parseAmount(amountStr)

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

            } catch (e: Exception) {
                Log.e(TAG, "parseNubank: error on line ${index + 2}: $line", e)
                return CsvParseResult.Error("Erro na linha ${index + 2}: ${e.message}", index + 2)
            }
        }

        Log.d(TAG, "parseNubank: parsed ${items.size} items")
        return CsvParseResult.Success(items)
    }

    // Itaú format
    private fun parseItau(lines: List<String>): CsvParseResult {
        val items = mutableListOf<CsvBillItem>()
        val dataLines = skipHeader(lines, listOf("data", "lançamento", "valor"))
        Log.d(TAG, "parseItau: ${dataLines.size} data lines")

        for ((index, line) in dataLines.withIndex()) {
            if (line.isBlank()) continue

            try {
                val parts = line.split(",")
                if (parts.size < 3) {
                    Log.w(TAG, "parseItau: skipping line ${index + 2}, not enough fields (${parts.size}): $line")
                    continue
                }

                val description = if (parts.size == 3) {
                    parts[1].trim()
                } else {
                    // Handle case where description contains commas (unquoted)
                    parts.subList(1, parts.size - 1).joinToString(",").trim()
                }

                val date = parseDate(parts[0].trim())
                val amountStr = parts.last().trim()
                val amount = parseAmount(amountStr)

                val (installmentNumber, totalInstallments) = parseInstallments(description)

                if (amount > 0) {
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
                } else {
                    Log.d(TAG, "parseItau: skipping line ${index + 2} with non-positive amount=$amount")
                }
            } catch (e: Exception) {
                Log.e(TAG, "parseItau: error on line ${index + 2}: $line", e)
                return CsvParseResult.Error("Erro na linha ${index + 2}: ${e.message}", index + 2)
            }
        }

        Log.d(TAG, "parseItau: parsed ${items.size} items")
        return CsvParseResult.Success(items)
    }

    // Generic format: Date;Description;Amount (semicolon separated) or comma separated
    // Assumes US number format (dot as decimal separator)
    private fun parseGeneric(lines: List<String>): CsvParseResult {
        val items = mutableListOf<CsvBillItem>()
        val dataLines = if (lines.first().lowercase().contains("data") ||
                           lines.first().lowercase().contains("date")) {
            lines.drop(1)
        } else {
            lines
        }
        Log.d(TAG, "parseGeneric: ${dataLines.size} data lines")

        for ((index, line) in dataLines.withIndex()) {
            if (line.isBlank()) continue

            try {
                // Try semicolon first, then comma
                val parts = if (line.contains(";")) {
                    line.split(";")
                } else {
                    parse3FieldCsvLine(line, ',')
                }

                if (parts.size < 3) {
                    Log.w(TAG, "parseGeneric: skipping line ${index + 2}, not enough fields (${parts.size}): $line")
                    continue
                }

                val date = parseDate(parts[0].trim())
                val description = parts[1].trim()
                // US format: dot is decimal separator, just clean currency symbols
                val amountStr = parts[2].trim()
                    .replace("R$", "")
                    .replace("$", "")
                    .replace(",", "")
                    .trim()
                val amount = parseAmount(amountStr)

                val (installmentNumber, totalInstallments) = parseInstallments(description)

                if (amount > 0) {
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
                } else {
                    Log.d(TAG, "parseGeneric: skipping line ${index + 2} with non-positive amount=$amount")
                }
            } catch (e: Exception) {
                Log.e(TAG, "parseGeneric: error on line ${index + 2}: $line", e)
                return CsvParseResult.Error("Erro na linha ${index + 2}: ${e.message}", index + 2)
            }
        }

        Log.d(TAG, "parseGeneric: parsed ${items.size} items")
        return CsvParseResult.Success(items)
    }

    private fun skipHeader(lines: List<String>, headerKeywords: List<String>): List<String> {
        val firstLine = lines.firstOrNull()?.lowercase() ?: return lines
        return if (headerKeywords.any { firstLine.contains(it) }) {
            lines.drop(1)
        } else {
            lines
        }
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

    /**
     * Parse a CSV line for 3-field format (date, description, amount)
     * Handles cases where description contains commas but isn't quoted
     */
    private fun parse3FieldCsvLine(line: String, delimiter: Char): List<String> {
        // First try standard parsing (handles quoted fields)
        val standardParts = parseCsvLine(line, delimiter)

        // If we got exactly 3 parts, use them as-is
        if (standardParts.size == 3) {
            return standardParts
        }

        // If we got more than 3 parts, assume unquoted description with commas
        // Format: date,description (with commas),amount
        if (standardParts.size > 3) {
            val date = standardParts[0]
            val amount = standardParts.last()
            // Join all middle parts as description
            val description = standardParts.subList(1, standardParts.size - 1).joinToString(delimiter.toString())
            return listOf(date, description, amount)
        }

        // Less than 3 parts, return as-is (will fail validation)
        return standardParts
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, brazilianDateFormatter)
        } catch (e: Exception) {
            try {
                LocalDate.parse(dateStr, isoDateFormatter)
            } catch (e2: Exception) {
                Log.w(TAG, "parseDate: unrecognized date format '$dateStr'")
                throw IllegalArgumentException("Data inválida: $dateStr")
            }
        }
    }

    private fun parseAmount(amountStr: String): Long {
        val cleanAmount = amountStr
            .replace("R$", "")
            .replace("$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        val value = try {
            BigDecimal(cleanAmount)
        } catch (e: NumberFormatException) {
            Log.w(TAG, "parseAmount: invalid amount string '$amountStr' (cleaned='$cleanAmount')")
            throw IllegalArgumentException("Valor inválido: $amountStr")
        }

        val cents = value.setScale(0, RoundingMode.HALF_UP)
        return cents.toLong()
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
