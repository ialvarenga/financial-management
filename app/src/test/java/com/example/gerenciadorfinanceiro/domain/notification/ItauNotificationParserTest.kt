package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ItauNotificationParserTest {

    private lateinit var parser: ItauNotificationParser

    @Before
    fun setup() {
        parser = ItauNotificationParser()
    }

    @Test
    fun `canParse returns true for ITAU source`() {
        assertTrue(parser.canParse(NotificationSource.ITAU))
    }

    @Test
    fun `canParse returns false for other sources`() {
        assertFalse(parser.canParse(NotificationSource.NUBANK))
        assertFalse(parser.canParse(NotificationSource.GOOGLE_WALLET))
    }

    @Test
    fun `parse PIX received notification`() {
        val title = "PIX recebido"
        val text = "Você recebeu R$ 150,00 de João Silva"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.ITAU, result.source)
        assertEquals(15000L, result.amount) // 150.00 in cents
        assertEquals("PIX recebido", result.description)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(PaymentMethod.PIX, result.paymentMethod)
        assertEquals(timestamp, result.timestamp)
        assertNull(result.lastFourDigits)
    }

    @Test
    fun `parse PIX sent notification`() {
        val title = "PIX enviado"
        val text = "pix enviado de R$ 50,75 para Maria Santos, enviado com sucesso"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.ITAU, result.source)
        assertEquals(5075L, result.amount) // 50.75 in cents
        assertEquals("PIX enviado para Maria Santos", result.description)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(PaymentMethod.PIX, result.paymentMethod)
        assertEquals(timestamp, result.timestamp)
        assertNull(result.lastFourDigits)
    }

    @Test
    fun `parse credit card purchase notification`() {
        val title = "Compra aprovada"
        val text = "Compra aprovada de R$ 89,90 em Supermercado Extra no dia 15/01"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.ITAU, result.source)
        assertEquals(8990L, result.amount) // 89.90 in cents
        assertEquals("Supermercado Extra", result.description)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)
        assertEquals(timestamp, result.timestamp)
        assertNull(result.lastFourDigits)
    }

    @Test
    fun `parse returns null for unrecognized notification`() {
        val title = "Saldo disponível"
        val text = "Seu saldo atual é de R$ 1.000,00"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNull(result)
    }

    @Test
    fun `parse handles decimal amounts correctly`() {
        val title = "PIX recebido"
        val text = "PIX recebido de R$ 1.234,56"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        assertEquals(123456L, result!!.amount) // 1234.56 in cents
    }

    @Test
    fun `parse handles amounts with no decimal part`() {
        val title = "PIX recebido"
        val text = "PIX recebido de R$ 100"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        assertEquals(10000L, result!!.amount) // 100.00 in cents
    }
}
