package com.example.gerenciadorfinanceiro.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyVisualTransformationTest {

    private fun format(digits: String, negative: Boolean = false): String =
        CurrencyVisualTransformation(negative).filter(AnnotatedString(digits)).text.text

    @Test
    fun `formats digits by count from the right`() {
        assertEquals("", format(""))
        assertEquals("0,05", format("5"))
        assertEquals("0,12", format("12"))
        assertEquals("1,23", format("123"))
        assertEquals("12,34", format("1234"))
        assertEquals("123,45", format("12345"))
        assertEquals("1.234,56", format("123456"))
        assertEquals("12.345,67", format("1234567"))
        assertEquals("1.234.567,89", format("123456789"))
        assertEquals("12.345.678.901,23", format("1234567890123"))
    }

    @Test
    fun `negative flag prepends minus sign`() {
        assertEquals("-12,34", format("1234", negative = true))
        assertEquals("-1.234,56", format("123456", negative = true))
        assertEquals("-0,05", format("5", negative = true))
        assertEquals("", format("", negative = true))
    }

    @Test
    fun `offset mapping is monotonic in bounds and round trips`() {
        val samples = listOf("5", "12", "123", "1234", "12345", "123456", "1234567", "1234567890123")
        for (negative in listOf(false, true)) {
            for (digits in samples) {
                val transformed = CurrencyVisualTransformation(negative)
                    .filter(AnnotatedString(digits))
                val mapping = transformed.offsetMapping
                val transformedLength = transformed.text.length

                var previous = -1
                for (o in 0..digits.length) {
                    val t = mapping.originalToTransformed(o)
                    assertTrue("non-decreasing for '$digits' at $o", t >= previous)
                    assertTrue("in bounds for '$digits' at $o", t in 0..transformedLength)
                    assertEquals(
                        "round trip for '$digits' (negative=$negative) at $o",
                        o,
                        mapping.transformedToOriginal(t)
                    )
                    previous = t
                }
                for (t in 0..transformedLength) {
                    val o = mapping.transformedToOriginal(t)
                    assertTrue("reverse in bounds for '$digits' at $t", o in 0..digits.length)
                }
            }
        }
    }

    @Test
    fun `offset mapping spot checks for 123456`() {
        val mapping = CurrencyVisualTransformation(false)
            .filter(AnnotatedString("123456")).offsetMapping

        // "123456" -> "1.234,56", digit positions: 1(0) .(1) 2(2) 3(3) 4(4) ,(5) 5(6) 6(7)
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1))
        assertEquals(3, mapping.originalToTransformed(2))
        assertEquals(4, mapping.originalToTransformed(3))
        assertEquals(6, mapping.originalToTransformed(4))
        assertEquals(7, mapping.originalToTransformed(5))
        assertEquals(8, mapping.originalToTransformed(6))
        // Tap after "1." lands between original digits 0 and 1
        assertEquals(0, mapping.transformedToOriginal(1))
        // Ambiguous positions (before an inserted separator) resolve downward:
        // offset 5 sits before the comma, whose following digit maps to transformed 6
        assertEquals(3, mapping.transformedToOriginal(5))
    }

    @Test
    fun `normalize strips non digits leading zeros and caps length`() {
        assertEquals("123", normalizeCurrencyDigits("12a,3"))
        assertEquals("7", normalizeCurrencyDigits("007"))
        assertEquals("", normalizeCurrencyDigits("0"))
        assertEquals("", normalizeCurrencyDigits("000"))
        assertEquals("123456", normalizeCurrencyDigits("R$ 1.234,56"))
        assertEquals("", normalizeCurrencyDigits(""))
        assertEquals(MAX_CURRENCY_DIGITS, normalizeCurrencyDigits("9".repeat(30)).length)
    }
}
