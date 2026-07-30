package com.example.gerenciadorfinanceiro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

class CurrencyUtilsTest {

    @Test
    fun `digitsToCents parses digit strings as cents`() {
        assertEquals(1234L, "1234".digitsToCents())
        assertEquals(5L, "5".digitsToCents())
        assertEquals(9999999999999L, "9999999999999".digitsToCents())
        assertNull("".digitsToCents())
        assertNull("abc".digitsToCents())
    }

    @Test
    fun `toDigitsString drops sign and maps zero to empty`() {
        assertEquals("1234", 1234L.toDigitsString())
        assertEquals("1234", (-1234L).toDigitsString())
        assertEquals("5", 5L.toDigitsString())
        assertEquals("", 0L.toDigitsString())
    }

    @Test
    fun `round trip preserves absolute value`() {
        val values = listOf(1L, 99L, 100L, 1234L, 123456L, -1L, -1234L, -987654321L)
        for (v in values) {
            assertEquals(abs(v), v.toDigitsString().digitsToCents())
        }
    }
}
