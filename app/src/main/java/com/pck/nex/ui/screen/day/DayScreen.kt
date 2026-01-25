package com.pck.nex.ui.screen.day


import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pck.nex.NeXApp
import com.pck.nex.domain.model.Day
import com.pck.nex.domain.model.Task
import com.pck.nex.graphics.NexBackground
import com.pck.nex.graphics.NexBackgroundCanvas
import com.pck.nex.graphics.PatternType
import com.pck.nex.graphics.buildBackground
import com.pck.nex.notifications.TaskAlarmScheduler
import com.pck.nex.ui.UiColors
import com.pck.nex.ui.uiColorsForBase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun DayScreen() {
    val context = LocalContext.current
    val repo = (context.applicationContext as NeXApp).repo
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var date by remember { mutableStateOf(LocalDate.now()) }
    var day by remember { mutableStateOf<Day?>(null) }

    val tasks by repo.tasks(date).collectAsState(initial = emptyList())

    LaunchedEffect(date) {
        day = repo.getOrCreate(date)
    }

    val today = LocalDate.now()
    val isPast = date.isBefore(today)
    val isToday = date == today
    val isFuture = date.isAfter(today)

    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    // Day is loaded asynchronously; guard first composition
    if (day == null) {
        val placeholderBg = NexBackground(
            seed = 0L,
            type = PatternType.DIAG_STRIPES,
            a = Color.Black,
            b = Color.Black
        )

        Box(Modifier.fillMaxSize()) {
            NexBackgroundCanvas(
                modifier = Modifier.fillMaxSize(),
                background = placeholderBg
            )
        }
        return
    }
    val resolvedDay = day!!

    // ---------- BACKGROUND ----------
    val bg = if (isFuture) {
        NexBackground(
            seed = 0L,
            type = PatternType.DIAG_STRIPES,
            a = Color.Black,
            b = Color.Black
        )
    } else {
        buildBackground(
            seed = resolvedDay.backgroundSeed,
            forcedTypeId = resolvedDay.backgroundType
        )
    }

    // ---------- UI COLORS ----------
    val ui = if (isFuture) {
        UiColors(
            text = Color.White,
            outline = Color.White,
            panelFill = Color.White.copy(alpha = 0.14f),
            fieldFill = Color.White.copy(alpha = 0.18f),
            buttonFill = Color.White.copy(alpha = 0.22f),
            buttonText = Color.White
        )
    } else uiColorsForBase(bg.a)

    var newTaskTitle by remember { mutableStateOf("") }
    val noRipple = remember { MutableInteractionSource() }

    val swipeThresholdPx = 80f
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = noRipple, indication = null) {
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
                .pointerInput(date) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum > swipeThresholdPx) {
                                focusManager.clearFocus(); keyboard?.hide()
                                date = date.minusDays(1)
                            } else if (dragAccum < -swipeThresholdPx) {
                                focusManager.clearFocus(); keyboard?.hide()
                                date = date.plusDays(1)
                            }
                            dragAccum = 0f
                        }
                    )
                }
        ) {

            // ---------- DATE CHIP ----------
            Surface(
                color = ui.panelFill,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateLabel, style = MaterialTheme.typography.titleLarge, color = ui.text)

                    TextButton(
                        enabled = isToday,
                        onClick = {
                            val seed = System.currentTimeMillis()
                            val type = (seed % PatternType.entries.size).toInt()
                            scope.launch {
                                repo.updateBackground(date, seed, type)
                                day = resolvedDay.copy(backgroundSeed = seed, backgroundType = type)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ui.text)
                    ) {
                        Text("↻")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---------- TASK LIST ----------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(tasks, key = { it.id }) { t ->
                    TaskRow(
                        context = context,
                        dayDate = date,
                        task = t,
                        enabled = !isPast,
                        ui = ui,
                        onToggle = { checked ->
                            scope.launch {
                                repo.upsertTask(date, t.copy(isDone = checked), tasks.indexOf(t))
                            }
                        },
                        onSetDueTime = { due ->
                            val updated = t.copy(dueTime = due)
                            scope.launch {
                                repo.upsertTask(date, updated, tasks.indexOf(t))
                            }
                            if (date == today) {
                                TaskAlarmScheduler.scheduleTaskAlarm(
                                    context,
                                    date,
                                    updated.title,
                                    stableRequestId(date, updated.id),
                                    LocalDateTime.of(date, due)
                                )
                            }
                        },
                        onClearDueTime = {
                            scope.launch {
                                repo.upsertTask(date, t.copy(dueTime = null), tasks.indexOf(t))
                            }
                            TaskAlarmScheduler.cancelTaskAlarm(
                                context,
                                stableRequestId(date, t.id)
                            )
                        },
                        onDelete = {
                            scope.launch { repo.deleteTask(t.id) }
                            TaskAlarmScheduler.cancelTaskAlarm(
                                context,
                                stableRequestId(date, t.id)
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---------- ADD BAR ----------
            Surface(
                color = ui.fieldFill,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("New task", color = ui.text.copy(alpha = 0.75f)) },
                        enabled = !isPast,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ui.text,
                            unfocusedTextColor = ui.text,
                            focusedBorderColor = ui.outline,
                            unfocusedBorderColor = ui.outline.copy(alpha = 0.65f),
                            cursorColor = ui.text,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val title = newTaskTitle.trim()
                                if (title.isNotEmpty()) {
                                    scope.launch {
                                        repo.upsertTask(
                                            date,
                                            Task(UUID.randomUUID(), title, false, null),
                                            tasks.size
                                        )
                                    }
                                    newTaskTitle = ""
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                }
                            }
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    Button(
                        enabled = !isPast && newTaskTitle.isNotBlank(),
                        onClick = {
                            val title = newTaskTitle.trim()
                            scope.launch {
                                repo.upsertTask(
                                    date,
                                    Task(UUID.randomUUID(), title, false, null),
                                    tasks.size
                                )
                            }
                            newTaskTitle = ""
                            focusManager.clearFocus()
                            keyboard?.hide()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ui.buttonFill,
                            contentColor = ui.buttonText
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Swipe left/right to change day",
                color = ui.text.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}


@Composable
private fun TaskRow(
    context: Context,
    dayDate: LocalDate,
    task: Task,
    enabled: Boolean,
    ui: UiColors,
    onToggle: (Boolean) -> Unit,
    onSetDueTime: (LocalTime) -> Unit,
    onClearDueTime: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = ui.panelFill,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { if (enabled) onToggle(it) },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = ui.text,
                    uncheckedColor = ui.text,
                    checkmarkColor = if (ui.text == Color.White) Color.Black else Color.White
                )
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = ui.text)

                val dueLabel = task.dueTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "No due time"
                Text(
                    dueLabel,
                    color = ui.text.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Small translucent button chips for actions
            ActionChip(
                text = "Time",
                enabled = enabled,
                ui = ui
            ) {
                val now = LocalTime.now()
                TimePickerDialog(
                    context,
                    { _, h, m -> onSetDueTime(LocalTime.of(h, m)) },
                    now.hour,
                    now.minute,
                    false
                ).show()
            }

            Spacer(Modifier.width(8.dp))

            if (task.dueTime != null) {
                ActionChip(text = "Clear", enabled = enabled, ui = ui) { onClearDueTime() }
                Spacer(Modifier.width(8.dp))
            }

            ActionChip(text = "Del", enabled = enabled, ui = ui) { onDelete() }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    enabled: Boolean,
    ui: UiColors,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ui.buttonFill,
            contentColor = ui.buttonText
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}


private fun stableRequestId(date: LocalDate, taskId: UUID): Int {
    return (date.toEpochDay().hashCode() xor taskId.hashCode()).absoluteValue
}
