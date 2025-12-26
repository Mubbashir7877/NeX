package com.pck.nex.ui.screen.day
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.pck.nex.data.inmemory.InMemoryDayStore
import com.pck.nex.domain.model.Task
import com.pck.nex.graphics.ContrastUtils
import com.pck.nex.graphics.NexBackgroundCanvas
import com.pck.nex.graphics.PatternType
import com.pck.nex.graphics.buildBackground
import com.pck.nex.notifications.TaskAlarmScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun DayScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var date by remember { mutableStateOf(LocalDate.now()) }
    var day by remember { mutableStateOf(InMemoryDayStore.getOrCreate(date)) }

    LaunchedEffect(date) {
        day = InMemoryDayStore.getOrCreate(date)
    }

    val isPast = date.isBefore(LocalDate.now())
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))

    val today = LocalDate.now()
    val isFuture = date.isAfter(today)

// FUTURE DAYS: pure black background until the day arrives
    val bg = if (isFuture) {
        com.pck.nex.graphics.NexBackground(
            seed = 0L,
            type = com.pck.nex.graphics.PatternType.DIAG_STRIPES, // unused
            a = androidx.compose.ui.graphics.Color.Black,
            b = androidx.compose.ui.graphics.Color.Black
        )
    } else {
        // Today or past: use stored background if present; otherwise generate ONCE for "today"
        val seed = day.backgroundSeed
        val typeId = day.backgroundType

        val resolvedSeed = seed ?: seedFrom(date, date.toEpochDay()) // stable for day start
        val resolvedType =
            typeId ?: ((resolvedSeed % com.pck.nex.graphics.PatternType.entries.size).toInt())

        // If it's today and background not yet assigned, assign it once (day-start behavior)
        if (date == today && (day.backgroundSeed == null || day.backgroundType == null)) {
            val newSeed = seedFrom(date, date.toEpochDay()) // deterministic per day
            val newType = (newSeed % com.pck.nex.graphics.PatternType.entries.size).toInt()
            val updated = day.copy(backgroundSeed = newSeed, backgroundType = newType)
            day = InMemoryDayStore.save(updated)
        }

        buildBackground(resolvedSeed, forcedTypeId = resolvedType)
    }

// Text/checkbox color: must contrast against BOTH colors
    val (autoTextColor, worstScore) =
        if (isFuture) {
            androidx.compose.ui.graphics.Color.White to 999.0
        } else {
            ContrastUtils.bestTextOnDualWithScore(bg.a, bg.b)
        }

    val textColor = autoTextColor

// If worst-case contrast is weak, use a scrim behind the content.
// 4.5 is WCAG AA for normal text; you can tune this up/down.
    val useScrim = !isFuture && worstScore < 4.5

    val scrimColor =
        if (!useScrim) androidx.compose.ui.graphics.Color.Transparent
        else if (textColor == androidx.compose.ui.graphics.Color.White)
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
        else
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f)


    // Bottom add bar state
    var newTaskTitle by remember { mutableStateOf("") }

    // Tap outside to clear focus + hide keyboard (removes IME icons)
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = noRipple,
                indication = null
            ) {
                focusManager.clearFocus()
                keyboard?.hide()
            }
    ) {
        NexBackgroundCanvas(
            modifier = Modifier.fillMaxSize(),
            background = bg
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // SCRIM PANEL (only visible when contrast is weak)
            Surface(
                color = scrimColor,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {

                    /* ---------- HEADER ---------- */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboard?.hide()
                                date = date.minusDays(1)
                            }
                        ) {
                            Text("Prev", color = textColor)
                        }

                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor
                        )

                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboard?.hide()
                                date = date.plusDays(1)
                            }
                        ) {
                            Text("Next", color = textColor)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    /* ---------- TASK LIST (SCROLLING) ---------- */
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(day.tasks, key = { it.id }) { t ->
                            TaskRow(
                                context = context,
                                dayDate = date,
                                task = t,
                                enabled = !isPast,
                                textColor = textColor,
                                checkboxColor = textColor,
                                onToggle = { checked ->
                                    val updated = t.copy(isDone = checked)
                                    day = autosaveDay(date = date, updateTask = updated)
                                },
                                onSetDueTime = { due ->
                                    val updated = t.copy(dueTime = due)
                                    day = autosaveDay(date = date, updateTask = updated)

                                    // Only schedule alarms for today
                                    if (date == LocalDate.now()) {
                                        val triggerAt = LocalDateTime.of(date, due)
                                        TaskAlarmScheduler.scheduleTaskAlarm(
                                            context,
                                            date,
                                            updated.title,
                                            stableRequestId(date, updated.id),
                                            triggerAt
                                        )
                                    }
                                },
                                onClearDueTime = {
                                    val updated = t.copy(dueTime = null)
                                    day = autosaveDay(date = date, updateTask = updated)
                                    TaskAlarmScheduler.cancelTaskAlarm(
                                        context,
                                        stableRequestId(date, t.id)
                                    )
                                },
                                onDelete = {
                                    TaskAlarmScheduler.cancelTaskAlarm(
                                        context,
                                        stableRequestId(date, t.id)
                                    )
                                    day = autosaveDay(date = date, deleteTaskId = t.id)
                                }
                            )
                        }
                    }

                    /* ---------- MANUAL REGENERATE (TODAY ONLY) ---------- */
                    if (date == LocalDate.now()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = {
                                    val newSeed = System.currentTimeMillis()
                                    val newType =
                                        (newSeed % PatternType.entries.size).toInt()
                                    day = InMemoryDayStore.save(
                                        day.copy(
                                            backgroundSeed = newSeed,
                                            backgroundType = newType
                                        )
                                    )
                                }
                            ) {
                                Text("↻", color = textColor)
                            }
                        }
                    }

                    /* ---------- ADD TASK BAR (BOTTOM) ---------- */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("New task") },
                            enabled = !isPast,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val title = newTaskTitle.trim()
                                    if (!isPast && title.isNotEmpty()) {
                                        val t = Task(title = title)
                                        day = autosaveDay(date = date, updateTask = t)
                                        newTaskTitle = ""
                                        focusManager.clearFocus()
                                        keyboard?.hide()
                                    }
                                }
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        Button(
                            enabled = !isPast && newTaskTitle.isNotBlank(),
                            onClick = {
                                val title = newTaskTitle.trim()
                                if (title.isNotEmpty()) {
                                    val t = Task(title = title)
                                    day = autosaveDay(date = date, updateTask = t)
                                    newTaskTitle = ""
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

}

        /**
 * Centralized autosave.
 * - Applies changes to the in-memory store
 * - Regenerates and stores a new backgroundSeed on each save event
 */
private fun autosaveDay(
    date: LocalDate,
    updateTask: Task? = null,
    deleteTaskId: UUID? = null
): com.pck.nex.domain.model.Day {
    var d = InMemoryDayStore.getOrCreate(date)
    if (updateTask != null) d = InMemoryDayStore.upsertTask(date, updateTask)
    if (deleteTaskId != null) d = InMemoryDayStore.deleteTask(date, deleteTaskId)
    return InMemoryDayStore.save(d) // NO background change here
}


@Composable
private fun TaskRow(
    context: Context,
    dayDate: LocalDate,
    task: Task,
    checkboxColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    onToggle: (Boolean) -> Unit,
    onSetDueTime: (LocalTime) -> Unit,
    onClearDueTime: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { if (enabled) onToggle(it) },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = checkboxColor,
                uncheckedColor = checkboxColor,
                checkmarkColor = if (checkboxColor == androidx.compose.ui.graphics.Color.White)
                    androidx.compose.ui.graphics.Color.Black
                else
                    androidx.compose.ui.graphics.Color.White
            )
        )


        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = textColor)
            val dueLabel = task.dueTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "No due time"
            Text(dueLabel, color = textColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }

        TextButton(
            enabled = enabled,
            onClick = {
                val now = LocalTime.now()
                TimePickerDialog(
                    context,
                    { _, h, m -> onSetDueTime(LocalTime.of(h, m)) },
                    now.hour,
                    now.minute,
                    false
                ).show()
            }
        ) { Text("Time", color = textColor) }

        if (task.dueTime != null) {
            TextButton(enabled = enabled, onClick = onClearDueTime) { Text("Clear", color = textColor) }
        }

        TextButton(enabled = enabled, onClick = onDelete) { Text("Del", color = textColor) }
    }
}

private fun seedFrom(date: LocalDate, millis: Long): Long {
    return (date.toEpochDay() * 31L) + (millis % Int.MAX_VALUE)
}

private fun stableRequestId(date: LocalDate, taskId: UUID): Int {
    return (date.toEpochDay().hashCode() xor taskId.hashCode()).absoluteValue
}
