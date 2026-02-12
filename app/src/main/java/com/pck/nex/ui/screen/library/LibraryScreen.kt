package com.pck.nex.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pck.nex.NeXApp
import com.pck.nex.data.repo.DayRepository
import com.pck.nex.graphics.NexBackgroundCanvas
import com.pck.nex.graphics.PatternType
import com.pck.nex.graphics.buildBackground
import com.pck.nex.ui.UiColors
import com.pck.nex.ui.components.BottomDockBar
import com.pck.nex.ui.components.DockTab
import com.pck.nex.ui.components.LibrarySettingsOverlay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable


fun LibraryScreen(
    onOpenDay: (String) -> Unit,
    onArchive: () -> Unit = {},
    onTemplate: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as NeXApp
    var currentTab by remember { mutableStateOf(DockTab.LIBRARY) }
    val dayRepo = app.dayRepo
    val templateRepo = app.templateRepo

    val scope = rememberCoroutineScope()

    val matteGray = Color(0xFFE6E6E6)
    val ui = UiColors(
        text = Color(0xFF121212),
        outline = Color(0xFF1A1A1A).copy(alpha = 0.35f),
        panelFill = Color.White.copy(alpha = 0.70f),
        fieldFill = Color.White.copy(alpha = 0.85f),
        buttonFill = Color.White.copy(alpha = 0.90f),
        buttonText = Color(0xFF121212)
    )

    var sort by remember { mutableStateOf(DayRepository.LibrarySort.NEWEST_TO_OLDEST) }
    val days by dayRepo.libraryDays(sort).collectAsState(initial = emptyList())

    val snack = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val sortLabel = when (sort) {
        DayRepository.LibrarySort.NEWEST_TO_OLDEST -> "Newest"
        DayRepository.LibrarySort.OLDEST_TO_NEWEST -> "Oldest"
        DayRepository.LibrarySort.TASKS_MOST_TO_LEAST -> "Tasks"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(matteGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // ───── TOP BAR ─────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ui.text
                )

                Row(verticalAlignment = Alignment.CenterVertically) {

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = sortLabel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .width(130.dp)
                                .height(52.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            shape = RoundedCornerShape(14.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedTextColor = ui.text,
                                unfocusedTextColor = ui.text
                            ),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest → Oldest") },
                                onClick = {
                                    sort = DayRepository.LibrarySort.NEWEST_TO_OLDEST
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest → Newest") },
                                onClick = {
                                    sort = DayRepository.LibrarySort.OLDEST_TO_NEWEST
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tasks (Most → Least)") },
                                onClick = {
                                    sort = DayRepository.LibrarySort.TASKS_MOST_TO_LEAST
                                    expanded = false
                                }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // ⚙️ SETTINGS BUTTON
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { showSettings = !showSettings },
                        shape = RoundedCornerShape(14.dp),
                        color = ui.panelFill
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "≡",
                                color = ui.text,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ───── GRID ─────
            if (currentTab == DockTab.LIBRARY) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(days, key = { it.date.toString() }) { d ->
                        DayTile(
                            dateIso = d.date.toString(),
                            seed = d.backgroundSeed,
                            typeId = d.backgroundType,
                            taskCount = d.taskCount,
                            onClick = { onOpenDay(d.date.toString()) }
                        )
                    }
                }

            } else {

                TemplateLibraryScreen()

            }

        }

        // ───── FLOATING DOCK ─────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            BottomDockBar(
                selected = currentTab,
                ui = ui,
                onLibrary = { currentTab = DockTab.LIBRARY },
                onArchive = {
                    scope.launch { snack.showSnackbar("Archive coming soon") }
                    onArchive()
                },
                onTemplate = { currentTab = DockTab.TEMPLATE }
            )

        }

        SnackbarHost(
            hostState = snack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // ───── SETTINGS OVERLAY ─────
        if (showSettings) {
            LibrarySettingsOverlay(
                ui = ui,
                onPickBackground = {
                    // next step: image picker + persistence
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun DayTile(
    dateIso: String,
    seed: Long,
    typeId: Int,
    taskCount: Int,
    onClick: () -> Unit
) {
    val isFuture = runCatching {
        java.time.LocalDate.parse(dateIso).isAfter(java.time.LocalDate.now())
    }.getOrDefault(false)

    val bg = if (isFuture) {
        com.pck.nex.graphics.NexBackground(
            seed = 0L,
            type = PatternType.DIAG_STRIPES,
            a = Color.Black,
            b = Color.Black
        )
    } else {
        buildBackground(seed = seed, forcedTypeId = typeId)
    }

    val dateLabel = runCatching {
        val d = java.time.LocalDate.parse(dateIso)
        d.format(DateTimeFormatter.ofPattern("MMM d"))
    }.getOrDefault(dateIso)

    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        NexBackgroundCanvas(
            modifier = Modifier.fillMaxSize(),
            background = bg
        )

        Surface(
            color = Color.Black.copy(alpha = 0.28f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = dateLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Surface(
            color = Color(0xFFFFA726),
            shape = RoundedCornerShape(topEnd = 14.dp),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = taskCount.toString(),
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }


}
