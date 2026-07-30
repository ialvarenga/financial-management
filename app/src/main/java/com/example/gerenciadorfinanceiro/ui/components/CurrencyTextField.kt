package com.example.gerenciadorfinanceiro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions

// Cap below Long overflow: up to R$ 99.999.999.999,99
const val MAX_CURRENCY_DIGITS = 13

/**
 * Enforces the field invariant: digits only, no leading zeros, bounded length.
 * Typing "0" on an empty field is a no-op (ATM behavior).
 */
fun normalizeCurrencyDigits(input: String): String =
    input.filter { it.isDigit() }.trimStart('0').take(MAX_CURRENCY_DIGITS)

/**
 * Formats a digits-only string as pt-BR currency by digit count from the right:
 * "5" -> "0,05", "1234" -> "12,34", "123456" -> "1.234,56".
 * The offset mapping is built while formatting so it is monotonic and in-bounds
 * by construction.
 */
class CurrencyVisualTransformation(
    private val negative: Boolean = false
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }
        val padded = digits.padStart(3, '0')
        val padCount = padded.length - digits.length
        val intLen = padded.length - 2
        val sb = StringBuilder()
        // origToTrans[o] = transformed offset for original offset o (0..digits.length)
        val origToTrans = IntArray(digits.length + 1)
        var t = 0
        if (negative) {
            sb.append('-')
            t++
        }
        for (p in padded.indices) {
            if (p in 1 until intLen && (intLen - p) % 3 == 0) {
                sb.append('.')
                t++
            }
            if (p == intLen) {
                sb.append(',')
                t++
            }
            val orig = p - padCount
            if (orig >= 0) origToTrans[orig] = t
            sb.append(padded[p])
            t++
        }
        origToTrans[digits.length] = t

        return TransformedText(AnnotatedString(sb.toString()), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, digits.length)]

            override fun transformedToOriginal(offset: Int): Int {
                var o = digits.length
                while (o > 0 && origToTrans[o] > offset) o--
                return o
            }
        })
    }

    override fun equals(other: Any?): Boolean =
        other is CurrencyVisualTransformation && other.negative == negative

    override fun hashCode(): Int = negative.hashCode()
}

/**
 * Currency input where the user types digits and the value is interpreted by
 * digit count: "1234" reads as R$ 12,34. State is the digits-only string
 * (which equals the unsigned cents value); parse it with String.digitsToCents().
 * When [allowNegative] is set, a +/- trailing toggle controls [isNegative] via
 * [onSignChange]; the sign is rendered but never part of the text.
 */
@Composable
fun CurrencyTextField(
    valueDigits: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(Icons.Default.AttachMoney, contentDescription = null)
    },
    enabled: Boolean = true,
    allowNegative: Boolean = false,
    isNegative: Boolean = false,
    onSignChange: ((Boolean) -> Unit)? = null
) {
    OutlinedTextField(
        value = valueDigits,
        onValueChange = { onValueChange(normalizeCurrencyDigits(it)) },
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text("0,00") },
        visualTransformation = remember(isNegative) {
            CurrencyVisualTransformation(negative = isNegative)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = if (allowNegative && onSignChange != null) {
            {
                IconButton(onClick = { onSignChange(!isNegative) }) {
                    Text(
                        text = if (isNegative) "−" else "+",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        } else null
    )
}
