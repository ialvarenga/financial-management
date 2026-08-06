package com.example.gerenciadorfinanceiro.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Single, shared date picker dialog for the whole app.
 *
 * Material 3's [DatePicker] keeps its state as "UTC midnight" milliseconds. If a
 * selection is decoded with the system time zone (any zone behind UTC, e.g. Brazil at
 * UTC-3), that UTC midnight lands on the previous calendar day, which made the picker
 * "jump back one day". To avoid that, both the initial value and the selection are
 * encoded/decoded strictly in UTC here, so the calendar day the user taps is the day
 * that is reported back. All date pickers must go through this composable so a fix here
 * applies everywhere.
 *
 * The selected [LocalDate] is reported once, when the user confirms with "OK".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Selecione a data"
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title, modifier = Modifier.padding(16.dp)) }
        )
    }
}
