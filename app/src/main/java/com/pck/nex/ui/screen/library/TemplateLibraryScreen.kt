package com.pck.nex.ui.screen.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pck.nex.NeXApp
import com.pck.nex.domain.model.Template
import com.pck.nex.domain.model.TemplateTask
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Template Library UI
 *
 * - Left: Templates list + Add New
 * - Right: Selected template tasks + actions
 *
 * Pure UI:
 *  - no repository access
 *  - no suspend lambdas
 */
@Composable
fun TemplateLibraryScreen(
    templates: List<Template>,
    tasksForSelected: List<TemplateTask>,
    selectedTemplateId: String?,

    onSelectTemplate: (String) -> Unit,
    onCreateTemplate: (String) -> Unit,
    onRenameTemplate: (String, String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAddTask: (String, String) -> Unit,
    onDeleteTask: (String, String) -> Unit,
    onAddToDay: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var newNameInput by remember { mutableStateOf("") }
    var taskInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        /* -----------------------------
         * LEFT: Template List
         * ----------------------------- */
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        ) {
            Text("Templates", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCreateDialog = true }
            ) {
                Text("Add New")
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = templates,
                    key = { it.templateId }
                ) { t ->
                    val isSelected = t.templateId == selectedTemplateId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelectTemplate(t.templateId) },
                        colors = CardDefaults.cardColors(
                            containerColor =
                            if (isSelected)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Updated",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        /* -----------------------------
         * RIGHT: Template Details
         * ----------------------------- */
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        ) {
            Text("Template Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (selectedTemplateId == null) {
                Text("Select a template to view or edit tasks.")
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { showRenameDialog = true }) {
                    Text("Edit Name")
                }

                Button(onClick = { onAddToDay(selectedTemplateId) }) {
                    Text("Add to Day")
                }

                OutlinedButton(onClick = { showDeleteDialog = true }) {
                    Text("Delete")
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ---- Add Task ---- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("New task") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        val title = taskInput.trim()
                        if (title.isNotEmpty()) {
                            onAddTask(selectedTemplateId, title)
                            taskInput = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Tasks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = tasksForSelected,
                    key = { it.taskId }
                ) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(task.title)
                            TextButton(
                                onClick = {
                                    onDeleteTask(task.taskId, selectedTemplateId)
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    /* -----------------------------
     * Dialogs
     * ----------------------------- */

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Template") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Template name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val n = nameInput.trim()
                    if (n.isNotEmpty()) {
                        onCreateTemplate(n)
                    }
                    nameInput = ""
                    showCreateDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Template") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val n = newNameInput.trim()
                    if (n.isNotEmpty()) {
                        onRenameTemplate(selectedTemplateId!!, n)
                    }
                    newNameInput = ""
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Template?") },
            text = { Text("This will delete the template and all its tasks.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteTemplate(selectedTemplateId!!)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Non-pure wrapper that preserves the "updates" logic:
 * - pulls repo from NeXApp
 * - observes templates + tasks
 * - performs repo calls inside scope.launch
 *
 * IMPORTANT: Does not change/remove any names/logic from either version.
 */
@Composable
fun TemplateLibraryScreen() {

    val context = LocalContext.current
    val app = context.applicationContext as NeXApp
    val templateRepo = app.templateRepo

    val scope = rememberCoroutineScope()

    val templates by templateRepo.observeTemplates()
        .collectAsState(initial = emptyList())

    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    val tasks by remember(selectedTemplateId) {
        selectedTemplateId?.let {
            templateRepo.observeTemplateTasks(it)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    TemplateLibraryScreen(
        templates = templates,
        tasksForSelected = tasks,
        selectedTemplateId = selectedTemplateId,

        onSelectTemplate = { id ->
            selectedTemplateId = id
        },
        onCreateTemplate = { name ->
            scope.launch {
                templateRepo.createTemplate(name)
            }
        },
        onRenameTemplate = { templateId, newName ->
            scope.launch {
                templateRepo.renameTemplate(templateId, newName)
            }
        },
        onDeleteTemplate = { templateId ->
            scope.launch {
                templateRepo.deleteTemplate(templateId)
                if (selectedTemplateId == templateId) selectedTemplateId = null
            }
        },
        onAddTask = { templateId, title ->
            scope.launch {
                templateRepo.addTask(templateId, title)
            }
        },
        onDeleteTask = { taskId, templateId ->
            scope.launch {
                templateRepo.deleteTask(taskId, templateId)
            }
        },
        onAddToDay = { templateId ->
            scope.launch {
                templateRepo.addTemplateToDay(templateId, LocalDate.now())
            }
        }
    )
}
