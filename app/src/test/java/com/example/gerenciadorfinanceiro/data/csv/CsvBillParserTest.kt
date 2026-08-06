package com.example.gerenciadorfinanceiro.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class CsvBillParserTest {

    private val parser = CsvBillParser()

    @Test
    fun parseCustomHappyPath() {
        // Amount in column 2, description in column 3, Brazilian decimals, 2 header rows
        val csvData = """
            Fatura do cartão - Junho 2026
            Data;Valor;Descrição
            07/06/2026;1.234,56;SUPERMERCADO GUANABARA
            06/06/2026;18,00;RESTAURANTE XYZ
        """.trimIndent()

        val config = CsvImportConfig(
            dateColumn = 1,
            descriptionColumn = 3,
            amountColumn = 2,
            skipRows = 2,
            delimiter = CsvDelimiter.SEMICOLON,
            datePattern = "dd/MM/yyyy",
            decimalStyle = DecimalStyle.BRAZILIAN
        )

        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        assertTrue("Result should be success", result is CsvParseResult.Success)
        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(123456L, success.items[0].amount)
        assertEquals("SUPERMERCADO GUANABARA", success.items[0].description)
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
        assertEquals(1800L, success.items[1].amount)
        assertTrue(success.skippedLines.isEmpty())
    }

    @Test
    fun parseCustomSkipsJunkFooter() {
        val csvData = """
            Data;Descrição;Valor
            07/06/2026;COMPRA A;10,00
            06/06/2026;COMPRA B;20,00
            Total da fatura
            Emitido em 30/06/2026 pelo banco
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 1)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        assertTrue("Result should be success", result is CsvParseResult.Success)
        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(2, success.skippedLines.size)
        assertEquals(4, success.skippedLines[0].lineNumber)
        assertEquals(5, success.skippedLines[1].lineNumber)
    }

    @Test
    fun parseCustomNegativeSkip() {
        val csvData = """
            07/06/2026;COMPRA;10,00
            06/06/2026;ESTORNO;-5,00
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 0, negativeHandling = NegativeHandling.SKIP)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(1, success.items.size)
        assertEquals(1, success.skippedLines.size)
        assertEquals("Valor negativo ignorado", success.skippedLines[0].reason)
    }

    @Test
    fun parseCustomNegativeKeep() {
        val csvData = """
            07/06/2026;COMPRA;10,00
            06/06/2026;ESTORNO;-5,00
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 0, negativeHandling = NegativeHandling.KEEP)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(-500L, success.items[1].amount)
    }

    @Test
    fun parseCustomUsDecimalStyle() {
        val csvData = """
            2026-06-07,PURCHASE,1234.56
            2026-06-06,PURCHASE B,"R$ 1,000.00"
        """.trimIndent()

        val config = CsvImportConfig(
            skipRows = 0,
            delimiter = CsvDelimiter.COMMA,
            datePattern = "yyyy-MM-dd",
            decimalStyle = DecimalStyle.US
        )
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(123456L, success.items[0].amount)
        assertEquals(100000L, success.items[1].amount)
    }

    @Test
    fun parseCustomTwoDigitYear() {
        val csvData = "07/06/26;COMPRA;10,00"

        val config = CsvImportConfig(skipRows = 0, datePattern = "dd/MM/yy")
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
    }

    @Test
    fun parseCustomTabDelimiter() {
        val csvData = "07/06/2026\tCOMPRA\t10,00"

        val config = CsvImportConfig(skipRows = 0, delimiter = CsvDelimiter.TAB)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(1, success.items.size)
        assertEquals(1000L, success.items[0].amount)
    }

    @Test
    fun parseCustomAllLinesFailReturnsError() {
        // Configured for semicolon but the file uses commas
        val csvData = """
            07/06/2026,COMPRA,10.00
            06/06/2026,COMPRA B,20.00
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 0, delimiter = CsvDelimiter.SEMICOLON)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        assertTrue("Result should be error", result is CsvParseResult.Error)
    }

    @Test
    fun parseCustomColumnBeyondPartsIsSkipped() {
        val csvData = """
            07/06/2026;COMPRA;10,00;extra
            06/06/2026;COMPRA B
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 0)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), config)

        val success = result as CsvParseResult.Success
        assertEquals(1, success.items.size)
        assertEquals(1, success.skippedLines.size)
        assertEquals(2, success.skippedLines[0].lineNumber)
    }
}
