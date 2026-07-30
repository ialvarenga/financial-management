package com.example.gerenciadorfinanceiro.data.csv

import com.example.gerenciadorfinanceiro.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class CsvBillParserTest {

    private val parser = CsvBillParser()

    @Test
    fun parseItauTest() {

        val csvData = """
            data,lançamento,valor
            2026-06-07,SUPERMARKET C GRANDERIO DE JANEIRBRA,41.94
            2026-06-06,CIRCO VOADORRIO DE JANEIRBRA,18
            2026-06-06,CIRCO VOADORRIO DE JANEIRBRA,34
            2026-06-03,IFD*TORRE E CIA SUPERMRIO DE JANEIRBRA,91.1
            2026-06-03,SUPERMARKET C GRANDERIO DE JANEIRBRA,51.23
            2026-06-02,SUPERMARKET C GRANDERIO DE JANEIRBRA,13.19
            2026-06-02,IFD*BORA VACA RESTAURARIO DE JANEIRBRA,67.79
            2026-06-01,SUPERMARKET C GRANDERIO DE JANEIRBRA,58.61
            2026-05-31,SUPERMARKET C GRANDERIO DE JANEIRBRA,42.77
            2026-05-31,GITHUB, INC.SAN FRANCISCOUSA,53.6
            2026-05-31,IOF INTERNACIONAL - GITHUB, INC.SAN FRANCISCOUSA,1.88
            2026-05-30,SUPERMERCADO GUANABARARIO DE JANEIRBRA,209.72
            2026-05-30,CHICKEN   BEER JACAREIJacareiBRA,38.98
            2026-05-30,CosechasRIO DE JANEIRBRA,30
            2026-05-30,SUPERMARKET C GRANDERIO DE JANEIRBRA,5.59
            2026-05-29,SUPERMARKET C GRANDERIO DE JANEIRBRA,33.54
            2026-05-28,AMAZON BRSAO PAULOBRA,46.9
            2026-05-28,IFD*IFOOD CLUBOsascoBRA,7.98
            2026-05-28,SUPERMARKET C GRANDERIO DE JANEIRBRA,31.51
            2026-05-27,RAIA DROGASIL SARIO DE JANEIRBRA,19.98
            2026-05-27,SUPERMARKET C GRANDERIO DE JANEIRBRA,47.43
            2026-05-27,ASSB COMERCIO VAREJISRIO DE JANEIRBRA,47.99
            2026-05-27,ASSB COMERCIO VAREJISRIO DE JANEIRBRA,23.5
            2026-05-27,MCDONALDS GNGRIO DE JANEIRBRA,38.8
            2026-05-26,SUPERMARKET C GRANDERIO DE JANEIRBRA,23.1
            2026-05-26,BOLERIA LOVE SUGARRIO DE JANEIRBRA,71
            2026-05-26,CACTUS ENTRETERIMENTORIO DE JANEIRBRA,61.23
            2026-05-25,SUPERMARKET C GRANDERIO DE JANEIRBRA,21.16
            2026-05-25,MERCADOLIVRE*MERCADOLOsascoBRA,-1754.06
            2026-05-25,SUPERMARKET C GRANDERIO DE JANEIRBRA,68.02
            2026-05-25,IFD*IFOOD CLUBOsascoBRA,7.98
            2026-05-25,SUPERMARKET C GRANDERIO DE JANEIRBRA,43.16
            2026-05-24,RAIA DROGASIL SARIO DE JANEIRBRA,27.69
            2026-05-24,ParkShopCampoGRjRIO DE JANEIRBRA,265.1
            2026-05-23,IFD*ASSB COMERCIO VARERIO DE JANEIRBRA,46.97
            2026-05-23,IFD*PET CENTER COMERCIRIO DE JANEIRBRA,102.14
            2026-05-23,DROGARIAS PACHECO S ARIO DE JANEIRBRA,24.98
            2026-05-23,SUPERMARKET C GRANDERIO DE JANEIRBRA,8.42
            2026-05-22,AmazonPrimeBRSAO PAULOBRA,19.9
            2026-05-22,SUPERMARKET C GRANDERIO DE JANEIRBRA,6.17
            2026-05-22,SUPERMARKET C GRANDERIO DE JANEIRBRA,17.1
            2026-05-22,SUPERMARKET C GRANDERIO DE JANEIRBRA,27.72
            2026-05-21,IFD*JALS FORNECIMENTORIO DE JANEIRBRA,63.63
            2026-05-21,BOLERIA LOVE SUGARRIO DE JANEIRBRA,35
            2026-05-21,CASA IMPERIALRIO DE JANEIRBRA,67.71
            2026-05-20,ZEDEK PIZZASRIO DE JANEIRBRA,72.7
            2026-05-20,IFD*PIZZARIA DA GRANDERIO DE JANEIRBRA,65.42
            2026-05-19,SUPERMARKET C GRANDERIO DE JANEIRBRA,65.32
            2026-05-18,RAIA DROGASIL SARIO DE JANEIRBRA,35.18
            2026-05-18,SUPERMARKET C GRANDERIO DE JANEIRBRA,83.89
            2026-05-17,MERCADOLIVRE*MERCADOLOsascoBRA,219.31
            2026-05-17,KALUNGA SHOP*Kalunga 0RIO DE JANEIRBRA,131.7
            2026-05-17,KOPENHAGEN PCGRIO DE JANEIRBRA,83.6
            2026-05-17,SUPERMARKET C GRANDERIO DE JANEIRBRA,46.51
            2026-05-16,ZS PIZZARIA1034RIO DE JANEIRBRA,34
            2026-05-16,IFD*ARCOS DOURADOS COMRIO DE JANEIRBRA,42.79
            2026-05-16,IFD*MERCATO EXPRESS HORIO DE JANEIRBRA,112.78
            2026-05-16,RAIA DROGASIL SARIO DE JANEIRBRA,66.96
            2026-05-16,SUPERMARKET C GRANDERIO DE JANEIRBRA,18.53
            2026-05-15,IFD*MM FOOD PIZZAS E MRIO DE JANEIRBRA,84.23
            2026-05-15,CASA IMPERIALRIO DE JANEIRBRA,55.36
            2026-05-15,SUPERMARKET C GRANDERIO DE JANEIRBRA,6.17
            2026-05-14,SUPERMARKET C GRANDERIO DE JANEIRBRA,83.85
            2026-05-14,SUPERMARKET C GRANDERIO DE JANEIRBRA,49.76
            2026-05-13,RAIA DROGASIL SARIO DE JANEIRBRA,30.47
            2026-05-13,SUPERMARKET C GRANDERIO DE JANEIRBRA,92.16
            2026-05-12,DELI PASTARIO DE JANEIRBRA,52
            2026-05-12,IFD*IFOOD CLUBOsascoBRA,12.9
            2026-05-11,TOTALPASSSAO PAULOBRA,149.9
            2026-05-11,RAIA DROGASIL SARIO DE JANEIRBRA,44.21
            2026-05-11,SUPERMARKET C GRANDERIO DE JANEIRBRA,79.02
            2026-05-10,PAGAMENTO COM SALDO,-15309.44
            2026-04-24,137.S.PARK CPO GDE - RRIO DE JANEIRBRA,59.93
            2026-04-24,TravelexRIO DE JANEIRBRA,225.72
            2026-04-12,DECATHLONRIO DE JANEIRBRA,196.98
            2026-04-11,KALUNGA SHOP*Kalunga 0RIO DE JANEIRBRA,106.63
            2026-04-06,parc  MP EMPSANTHOMAZOSASCOBRA,441.26
            2026-03-29,CL JOIASRIO DE JANEIRBRA,918.4
            2026-03-21,LU RODRIGUES - CAMPO GRIO DE JANEIRBRA,433.33
            2025-08-17,AMAZONMKTPLC*FASTSHOPS SAO PAULO     BRA,213.16
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray())
        val result = parser.parse(inputStream, CsvFormat.ITAU)

        assertTrue("Result should be success", result is CsvParseResult.Success)
        val items = (result as CsvParseResult.Success).items

        // 80 data lines, negatives (refunds/payments) are kept
        assertEquals(80, items.size)
    }

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

        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

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
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

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
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

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
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

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
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

        val success = result as CsvParseResult.Success
        assertEquals(2, success.items.size)
        assertEquals(123456L, success.items[0].amount)
        assertEquals(100000L, success.items[1].amount)
    }

    @Test
    fun parseCustomTwoDigitYear() {
        val csvData = "07/06/26;COMPRA;10,00"

        val config = CsvImportConfig(skipRows = 0, datePattern = "dd/MM/yy")
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

        val success = result as CsvParseResult.Success
        assertEquals(LocalDate.of(2026, 6, 7), success.items[0].date)
    }

    @Test
    fun parseCustomTabDelimiter() {
        val csvData = "07/06/2026\tCOMPRA\t10,00"

        val config = CsvImportConfig(skipRows = 0, delimiter = CsvDelimiter.TAB)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

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
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

        assertTrue("Result should be error", result is CsvParseResult.Error)
    }

    @Test
    fun parseCustomColumnBeyondPartsIsSkipped() {
        val csvData = """
            07/06/2026;COMPRA;10,00;extra
            06/06/2026;COMPRA B
        """.trimIndent()

        val config = CsvImportConfig(skipRows = 0)
        val result = parser.parse(ByteArrayInputStream(csvData.toByteArray()), CsvFormat.CUSTOM, config)

        val success = result as CsvParseResult.Success
        assertEquals(1, success.items.size)
        assertEquals(1, success.skippedLines.size)
        assertEquals(2, success.skippedLines[0].lineNumber)
    }
}
