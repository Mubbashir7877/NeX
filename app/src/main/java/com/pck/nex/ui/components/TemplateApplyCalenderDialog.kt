package com.pck.nex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateApplyCalendarDialog(
    onDismiss: () -> Unit,
    onApply: (List<LocalDate>) -> Unit
) {
    val rangeState = rememberDateRangePickerState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp) // prevents runaway height
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                // 🔹 HEADER (Fixed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "Select date range",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val startMillis = rangeState.selectedStartDateMillis
                                val endMillis = rangeState.selectedEndDateMillis

                                if (startMillis != null && endMillis != null) {

                                    val startDate = Instant
                                        .ofEpochMilli(startMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    val endDate = Instant
                                        .ofEpochMilli(endMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    val dates = mutableListOf<LocalDate>()
                                    var current = startDate
                                    while (!current.isAfter(endDate)) {
                                        dates.add(current)
                                        current = current.plusDays(1)
                                    }

                                    onApply(dates)
                                }
                            },
                            enabled = rangeState.selectedStartDateMillis != null &&
                                    rangeState.selectedEndDateMillis != null
                        ) {
                            Text("Apply")
                        }
                    }
                }

                Divider()

                // 🔹 CALENDAR (Scrollable area only)
                DateRangePicker(
                    state = rangeState,
                    showModeToggle = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // calendar takes remaining space
                )
            }
        }
    }
}