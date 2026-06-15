package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NubankNotificationParserTest {

    private lateinit var parser: NubankNotificationParser

    @Before
    fun setup() {
        parser = NubankNotificationParser()
    }

    @Test
    fun `canParse returns true for NUBANK source`() {
        assertTrue(parser.canParse(NotificationSource.NUBANK))
    }

    @Test
    fun `canParse returns false for other sources`() {
        assertFalse(parser.canParse(NotificationSource.ITAU))
        assertFalse(parser.canParse(NotificationSource.GOOGLE_WALLET))
    }

    @Test
    fun `parse bill payment notification`() {
        val title = "Fatura paga"
        val text = "Sua fatura do cartão de crédito foi paga com sucesso"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(0L, result.amount)
        assertEquals("Fatura paga", result.description)
        assertTrue(result.isBillPayment)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)
    }

    @Test
    fun `parse Nupay credit card purchase with installments`() {
        val title = "Compra aprovada com Nupay de R$ 300,00"
        val text = "Compra em 3x no Crédito APROVADA em Magazine Luiza"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(30000L, result.amount) // 300.00 in cents
        assertEquals("Magazine Luiza", result.description)
        assertEquals(3, result.installments)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)
        assertNull(result.transactionType) // Credit card doesn't have transaction type
    }

    @Test
    fun `parse Nupay debit card purchase`() {
        val title = "Compra aprovada com Nupay de R$ 45,50"
        val text = "Compra em 1x no Débito APROVADA em Padaria São Paulo"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(4550L, result.amount) // 45.50 in cents
        assertEquals("Padaria São Paulo", result.description)
        assertEquals(1, result.installments)
        assertEquals(PaymentMethod.DEBIT, result.paymentMethod)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
    }

    @Test
    fun `parse credit card purchase with last 4 digits`() {
        val title = "Compra aprovada"
        val text = "Compra de R$ 125,00 APROVADA em Netflix para o cartão com final 1234"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(12500L, result.amount) // 125.00 in cents
        assertEquals("Netflix", result.description)
        assertEquals("1234", result.lastFourDigits)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)
    }

    @Test
    fun `parse debit card purchase`() {
        val title = "Compra aprovada"
        val text = "Compra de R$ 78,90 APROVADA em Uber débito"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(7890L, result.amount) // 78.90 in cents
        assertEquals("Uber", result.description)
        assertEquals(PaymentMethod.DEBIT, result.paymentMethod)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
    }

    @Test
    fun `parse PIX reimbursement`() {
        val title = "Você recebeu um reembolso"
        val text = "Você recebeu um reembolso de R$ 50,00 de Amazon Brasil."
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(5000L, result.amount) // 50.00 in cents
        assertEquals("Reembolso PIX recebido de Amazon Brasil", result.description)
        assertEquals(PaymentMethod.PIX, result.paymentMethod)
        assertEquals(TransactionType.INCOME, result.transactionType)
    }

    @Test
    fun `parse transfer received`() {
        val title = "Transferência recebida"
        val text = "Você recebeu R$ 200,00 de Pedro Costa"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(20000L, result.amount) // 200.00 in cents
        assertEquals("Transferência recebida", result.description)
        assertEquals(PaymentMethod.PIX, result.paymentMethod)
        assertEquals(TransactionType.INCOME, result.transactionType)
    }

    @Test
    fun `parse transfer sent`() {
        val title = "Transferência enviada"
        val text = "Você enviou R$ 150,00 para Ana Silva"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNotNull(result)
        result!!
        assertEquals(NotificationSource.NUBANK, result.source)
        assertEquals(15000L, result.amount) // 150.00 in cents
        assertEquals("Transferência enviada", result.description)
        assertEquals(PaymentMethod.PIX, result.paymentMethod)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
    }

    @Test
    fun `parse returns null for unrecognized notification`() {
        val title = "Saldo disponível"
        val text = "Seu saldo atual é de R$ 1.000,00"
        val timestamp = System.currentTimeMillis()

        val result = parser.parse(title, text, timestamp)

        assertNull(result)
    }
}
