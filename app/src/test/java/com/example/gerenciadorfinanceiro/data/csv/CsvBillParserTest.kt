package com.example.gerenciadorfinanceiro.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
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

    @Test
    fun parseXlsxNumericDateAndAmount() {
        // Typical Excel export: header as shared strings, dates as serial numbers
        // (one with a time of day) and amounts as plain numbers with float noise
        val sheet = sheetXml(
            """<row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c></row>""",
            """<row r="2"><c r="A2" s="1"><v>46180</v></c><c r="B2" t="s"><v>3</v></c><c r="C2"><v>1234.56</v></c></row>""",
            """<row r="3"><c r="A3" s="1"><v>46179.75</v></c><c r="B3" t="s"><v>4</v></c><c r="C3"><v>18.000000000000004</v></c></row>"""
        )
        val file = xlsx(
            sheet,
            sharedStrings = listOf("Data", "Descrição", "Valor", "SUPERMERCADO GUANABARA", "NETFLIX 2/6")
        )

        val result = parser.parse(ByteArrayInputStream(file), CsvImportConfig(skipRows = 1))

        assertTrue("Result should be success", result is CsvParseResult.Success)
        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
        assertEquals("SUPERMERCADO GUANABARA", success.items[0].description)
        assertEquals(123456L, success.items[0].amount)
        assertEquals(LocalDate.of(2026, 6, 6), success.items[1].date)
        assertEquals(1800L, success.items[1].amount)
        assertEquals(2, success.items[1].installmentNumber)
        assertEquals(6, success.items[1].totalInstallments)
        assertTrue(success.skippedLines.isEmpty())
    }

    @Test
    fun parseXlsxTextCellsUseConfiguredFormats() {
        val sheet = sheetXml(
            """<row r="1"><c r="A1" t="inlineStr"><is><t>07/06/2026</t></is></c><c r="B1" t="inlineStr"><is><t>COMPRA</t></is></c><c r="C1" t="inlineStr"><is><t>R$ 1.234,56</t></is></c></row>"""
        )

        val result = parser.parse(
            ByteArrayInputStream(xlsx(sheet)),
            CsvImportConfig(skipRows = 0, datePattern = "dd/MM/yyyy", decimalStyle = DecimalStyle.BRAZILIAN)
        )

        val success = result as CsvParseResult.Success
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
        assertEquals(123456L, success.items[0].amount)
    }

    @Test
    fun parseXlsxIgnoresDelimiter() {
        val sheet = sheetXml(
            """<row r="1"><c r="A1"><v>46180</v></c><c r="B1" t="inlineStr"><is><t>COMPRA; COM, SEPARADORES</t></is></c><c r="C1"><v>10</v></c></row>"""
        )

        val result = parser.parse(
            ByteArrayInputStream(xlsx(sheet)),
            CsvImportConfig(skipRows = 0, delimiter = CsvDelimiter.COMMA)
        )

        val success = result as CsvParseResult.Success
        assertEquals("COMPRA; COM, SEPARADORES", success.items[0].description)
        assertEquals(1000L, success.items[0].amount)
    }

    @Test
    fun parseXlsxSkipRowsAndLineNumbersFollowSheetRows() {
        // Row 2 is empty, so it isn't stored in the file at all
        val sheet = sheetXml(
            """<row r="1"><c r="A1" t="inlineStr"><is><t>Fatura Junho</t></is></c></row>""",
            """<row r="3"><c r="A3" t="inlineStr"><is><t>Data</t></is></c><c r="B3" t="inlineStr"><is><t>Descrição</t></is></c><c r="C3" t="inlineStr"><is><t>Valor</t></is></c></row>""",
            """<row r="4"><c r="A4"><v>46180</v></c><c r="B4" t="inlineStr"><is><t>COMPRA A</t></is></c><c r="C4"><v>10</v></c></row>""",
            """<row r="5"><c r="A5"><v>46179</v></c><c r="B5" t="inlineStr"><is><t>ESTORNO</t></is></c><c r="C5"><v>-5</v></c></row>""",
            """<row r="7"><c r="A7" t="inlineStr"><is><t>Total</t></is></c><c r="C7"><v>5</v></c></row>"""
        )

        val result = parser.parse(
            ByteArrayInputStream(xlsx(sheet)),
            CsvImportConfig(skipRows = 3, negativeHandling = NegativeHandling.SKIP)
        )

        val success = result as CsvParseResult.Success
        assertEquals(1, success.items.size)
        assertEquals("COMPRA A", success.items[0].description)
        assertEquals(listOf(5, 7), success.skippedLines.map { it.lineNumber })
        assertEquals("Valor negativo ignorado", success.skippedLines[0].reason)
        assertEquals("Data inválida", success.skippedLines[1].reason)
        assertEquals("Total |  | 5", success.skippedLines[1].content)
    }

    @Test
    fun parseXlsxDate1904() {
        val sheet = sheetXml(
            """<row r="1"><c r="A1"><v>44718</v></c><c r="B1" t="inlineStr"><is><t>COMPRA</t></is></c><c r="C1"><v>10</v></c></row>"""
        )

        val result = parser.parse(ByteArrayInputStream(xlsx(sheet, date1904 = true)), CsvImportConfig(skipRows = 0))

        val success = result as CsvParseResult.Success
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
    }

    @Test
    fun parseLegacyXlsReturnsError() {
        val xls = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())

        val result = parser.parse(ByteArrayInputStream(xls), CsvImportConfig())

        assertTrue("Result should be error", result is CsvParseResult.Error)
        assertTrue((result as CsvParseResult.Error).message.contains(".xls"))
    }
}
