package com.example.gerenciadorfinanceiro.data.csv

enum class CsvDelimiter(val char: Char, val displayName: String) {
    COMMA(',', "Vírgula (,)"),
    SEMICOLON(';', "Ponto e vírgula (;)"),
    TAB('\t', "Tabulação (Tab)")
}

enum class DecimalStyle(val displayName: String) {
    BRAZILIAN("Brasileiro (1.234,56)"),
    US("Americano (1,234.56)")
}

enum class NegativeHandling(val displayName: String) {
    KEEP("Manter (estornos)"),
    SKIP("Ignorar")
}

/**
 * User-defined parameters for the "Personalizado" CSV import format.
 * Column indices are 1-based (1 = first column), converted to 0-based only at parse time.
 */
data class CsvImportConfig(
    val dateColumn: Int = 1,
    val descriptionColumn: Int = 2,
    val amountColumn: Int = 3,
    val skipRows: Int = 1,
    val delimiter: CsvDelimiter = CsvDelimiter.SEMICOLON,
    val datePattern: String = "dd/MM/yyyy",
    val decimalStyle: DecimalStyle = DecimalStyle.BRAZILIAN,
    val negativeHandling: NegativeHandling = NegativeHandling.SKIP
) {
    companion object {
        val DATE_PATTERNS = listOf("dd/MM/yyyy", "dd/MM/yy", "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy")
    }
}
