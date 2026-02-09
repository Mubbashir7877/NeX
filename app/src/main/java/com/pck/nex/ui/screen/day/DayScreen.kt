package com.pck.nex.ui.screen.day

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import coil.size.Size as CoilSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import coil.decode.GifDecoder
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pck.nex.NeXApp
import com.pck.nex.R
import com.pck.nex.widget.WidgetRefresher
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
fun DayScreen(
    initialDateIso: String,
    onOpenLibrary: () -> Unit,
    onOpenDay: (dateIso: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as NeXApp
    val repo = app.dayRepo
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snack = remember { SnackbarHostState() }

    var date by remember(initialDateIso) { mutableStateOf(LocalDate.parse(initialDateIso)) }
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

    // Safe placeholder while day loads
    if (day == null) {
        val placeholder = NexBackground(
            seed = 0L,
            type = PatternType.DIAG_STRIPES,
            a = Color.Black,
            b = Color.Black
        )
        Box(Modifier.fillMaxSize()) {
            NexBackgroundCanvas(Modifier.fillMaxSize(), placeholder)
        }
        return
    }

    val resolvedDay = day!!

    // Background rules:
    // - Future days: pure black
    // - Today/past: use stored background (Room)
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

    // Swipe nav thresholds
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
                            // Existing UX:
                            // swipe right -> previous day
                            // swipe left  -> next day
                            if (dragAccum > swipeThresholdPx) {
                                // PREVIOUS: only jump to previous day with tasks
                                focusManager.clearFocus(); keyboard?.hide()
                                scope.launch {
                                    val prev = repo.previousDayWithTasks(date)
                                    if (prev == null) {
                                        snack.showSnackbar("No previous tasks found")
                                    } else {
                                        onOpenDay(prev.toString())
                                    }
                                }
                            } else if (dragAccum < -swipeThresholdPx) {
                                focusManager.clearFocus(); keyboard?.hide()
                                onOpenDay(date.plusDays(1).toString())
                            }
                            dragAccum = 0f
                        }
                    )
                }
        ) {
            // DATE CHIP
            Surface(
                color = ui.panelFill,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = ui.text
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(enabled = isToday) {
                                val seed = System.currentTimeMillis()
                                val type = (seed % PatternType.entries.size).toInt()
                                scope.launch {
                                    repo.updateBackground(date, seed, type)
                                    day = day!!.copy(backgroundSeed = seed, backgroundType = type)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.raw.refresh)
                                .decoderFactory(GifDecoder.Factory())
                                .size(CoilSize.ORIGINAL)
                                .build(),
                            contentDescription = "Refresh background",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // TASK LIST
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
                            scope.launch { repo.upsertTask(date, t.copy(isDone = checked), tasks.indexOf(t)) }
                            if (date == LocalDate.now()) {
                                WidgetRefresher.refreshTodayWidget(context)
                            }

                        },
                        onSetDueTime = { due ->
                            val updated = t.copy(dueTime = due)
                            scope.launch { repo.upsertTask(date, updated, tasks.indexOf(t)) }
                            if (date == LocalDate.now()) {
                                WidgetRefresher.refreshTodayWidget(context)
                            }


                            if (date == LocalDate.now()) {
                                val triggerAt = LocalDateTime.of(date, due)
                                val requestId = stableRequestId(date, updated.id)
                                TaskAlarmScheduler.scheduleTaskAlarm(
                                    context = context,
                                    day = date,
                                    taskTitle = updated.title,
                                    requestId = requestId,
                                    triggerAt = triggerAt
                                )
                            }
                        },
                        onClearDueTime = {
                            scope.launch { repo.upsertTask(date, t.copy(dueTime = null), tasks.indexOf(t)) }
                            if (date == LocalDate.now()) {
                                WidgetRefresher.refreshTodayWidget(context)
                            }

                            TaskAlarmScheduler.cancelTaskAlarm(context, stableRequestId(date, t.id))
                        },
                        onDelete = {
                            scope.launch { repo.deleteTask(t.id) }
                            if (date == LocalDate.now()) {
                                WidgetRefresher.refreshTodayWidget(context)
                            }

                            TaskAlarmScheduler.cancelTaskAlarm(context, stableRequestId(date, t.id))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ADD BAR (Library button on left, Add button on right)
            Surface(
                color = ui.fieldFill,
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Library button (3-rectangle aesthetic)
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onOpenLibrary()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(ui.buttonFill, RoundedCornerShape(14.dp))
                    ) {
                        WindowsTileGlyph(color = ui.buttonText)
                    }

                    Spacer(Modifier.width(10.dp))

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
                            focusedLabelColor = ui.text.copy(alpha = 0.75f),
                            unfocusedLabelColor = ui.text.copy(alpha = 0.65f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val title = newTaskTitle.trim()
                                if (!isPast && title.isNotEmpty()) {
                                    scope.launch {
                                        repo.upsertTask(
                                            date,
                                            Task(UUID.randomUUID(), title, false, null),
                                            tasks.size
                                        )
                                        if (date == LocalDate.now()) {
                                            WidgetRefresher.refreshTodayWidget(context)
                                        }

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
                            if (title.isNotEmpty()) {
                                scope.launch {
                                    repo.upsertTask(
                                        date,
                                        Task(UUID.randomUUID(), title, false, null),
                                        tasks.size
                                    )
                                    if (date == LocalDate.now()) {
                                        WidgetRefresher.refreshTodayWidget(context)
                                    }

                                }
                                newTaskTitle = ""
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
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




        }

        SnackbarHost(
            hostState = snack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun WindowsTileGlyph(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = w * 0.18f
        val gap = w * 0.10f

        val cellW = (w - pad * 2f - gap) / 2f
        val cellH = (h - pad * 2f - gap) / 2f
        val r = CornerRadius(cellW * 0.18f, cellW * 0.18f)

        fun rect(x: Float, y: Float) = androidx.compose.ui.geometry.Rect(
            left = x,
            top = y,
            right = x + cellW,
            bottom = y + cellH
        )

        drawRoundRect(color, topLeft = Offset(pad, pad), size = Size(cellW, cellH), cornerRadius = r)
        drawRoundRect(color, topLeft = Offset(pad + cellW + gap, pad), size = Size(cellW, cellH), cornerRadius = r)
        drawRoundRect(color, topLeft = Offset(pad, pad + cellH + gap), size = Size(cellW, cellH), cornerRadius = r)
        drawRoundRect(color, topLeft = Offset(pad + cellW + gap, pad + cellH + gap), size = Size(cellW, cellH), cornerRadius = r)
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

            ActionChip(text = "Time", enabled = enabled, ui = ui) {
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
