package com.example.gerenciadorfinanceiro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationSourceTest {

    @Test
    fun `recognizes an Itaú app in the official package namespace`() {
        assertEquals(NotificationSource.ITAU, NotificationSource.fromPackageName("com.itau.iti"))
    }

    @Test
    fun `does not recognize unrelated packages`() {
        assertNull(NotificationSource.fromPackageName("com.example.unrelated"))
    }
}
