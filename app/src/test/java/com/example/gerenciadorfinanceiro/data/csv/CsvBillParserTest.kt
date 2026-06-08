package com.example.gerenciadorfinanceiro.data.csv

import com.example.gerenciadorfinanceiro.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvBillParserTest {

    private val parser = CsvBillParser()

    @Test
    fun `parseNubank should correctly parse valid CSV lines`() {
        // Sample Nubank CSV content - you can replace this with your actual data
        val csvData = """
            date,title,amount
            2026-06-07,Raia Drogasil,"10,99"
            2026-06-07,Auto Posto Inaja,"22,00"
            2026-06-07,Google Deezer Music P,"24,90"
            2026-06-07,Circo Voador,"50,00"
            2026-06-06,Uber - NuPay,"105,93"
            2026-06-06,Uber - NuPay,"141,96"
            2026-06-06,Supermarket C Grande,"22,57"
            2026-06-06,Viniciusmoura,"326,77"
            2026-06-06,Raia Drogasil,"24,97"
            2026-06-06,Eventim Brasil Sao Pau,"352,00"
            2026-06-05,Raia Drogasil - NuPay,"84,07"
            2026-06-05,Netflix.Com,"44,90"
            2026-06-04,Pagamento recebido,"- 7.188,73"
            2026-06-04,iFood - NuPay,"73,86"
            2026-06-04,iFood - NuPay,"70,79"
            2026-06-04,Uber - NuPay,"8,98"
            2026-06-04,Google Colab,"58,00"
            2026-06-04,Raia Drogasil - Parcela 2/2,"346,24"
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray())
        val result = parser.parse(inputStream, CsvFormat.NUBANK)

        assertTrue("Result should be success", result is CsvParseResult.Success)
        val items = (result as CsvParseResult.Success).items

        // Nubank logic in your class currently takes the absolute value and filters for amount > 0
        // Adjust these assertions based on your expected behavior
        assertEquals(3, items.size)
        
        assertEquals("iFood", items[0].description)
        assertEquals(10050L, items[0].amount) // 100.50 in cents
        assertEquals(Category.FOOD, items[0].category)

        assertEquals("Uber", items[1].description)
        assertEquals(2500L, items[1].amount)
        assertEquals(Category.TRANSPORT, items[1].category)
    }
    
    @Test
    fun `parseNubank should handle installments correctly`() {
        val csvData = """
            date,title,amount
            2023-11-01,Amazon parcela 2/5,-50.00
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray())
        val result = parser.parse(inputStream, CsvFormat.NUBANK)

        assertTrue(result is CsvParseResult.Success)
        val items = (result as CsvParseResult.Success).items
        
        assertEquals(1, items.size)
        assertEquals(2, items[0].installmentNumber)
        assertEquals(5, items[0].totalInstallments)
        assertEquals(Category.SHOPPING, items[0].category)
    }
}
