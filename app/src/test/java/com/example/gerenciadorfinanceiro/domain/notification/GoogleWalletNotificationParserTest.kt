package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GoogleWalletNotificationParserTest {

    private lateinit var parser: GoogleWalletNotificationParser

    @Before
    fun setup() {
        parser = GoogleWalletNotificationParser()
    }

    @Test
    fun `canParse returns true for GOOGLE_WALLET source`() {
        assertTrue(parser.canParse(NotificationSource.GOOGLE_WALLET))
    }

    @Test
    fun `canParse returns false for other sources`() {
        assertFalse(parser.canParse(NotificationSource.ITAU))
        assertFalse(parser.canParse(NotificationSource.NUBANK))
    }

    @Test
    fun `parse Google Wallet purchase notification`() {
        val title = "Starbucks Coffee"
        val text = "R$ 25,90 em seu cartão que termina em 5678"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.GOOGLE_WALLET, result.source)
        assertEquals(2590L, result.amount) // 25.90 in cents
        assertEquals("Starbucks Coffee", result.description)
        assertEquals("5678", result.lastFourDigits)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)
        assertEquals(timestamp, result.timestamp)
        assertNull(result.transactionType) // Credit card doesn't have transaction type
    }

    @Test
    fun `parse with larger amount`() {
        val title = "Amazon.com.br"
        val text = "R$ 1.350,00 em seu cartão que termina em 1234"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(135000L, result.amount) // 1350.00 in cents
        assertEquals("Amazon.com.br", result.description)
        assertEquals("1234", result.lastFourDigits)
    }

    @Test
    fun `parse with cents only`() {
        val title = "Padaria"
        val text = "R$ 5,50 em seu cartão que termina em 9999"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(550L, result.amount) // 5.50 in cents
    }

    @Test
    fun `parse uses default description when title is blank`() {
        val title = ""
        val text = "R$ 100,00 em seu cartão que termina em 4321"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals("Compra Google Wallet", result.description)
        assertEquals("4321", result.lastFourDigits)
    }

    @Test
    fun `parse returns null for unmatched pattern`() {
        val title = "Google Wallet"
        val text = "Seu cartão foi adicionado com sucesso"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNull(result)
    }

    @Test
    fun `parse returns null when amount parsing fails`() {
        val title = "Test Store"
        val text = "R$ ABC,XY em seu cartão que termina em 1234"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNull(result)
    }
}
