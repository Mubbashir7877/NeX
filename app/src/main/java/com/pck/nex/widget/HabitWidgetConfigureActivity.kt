package com.pck.nex.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.pck.nex.NeXApp
import kotlinx.coroutines.launch

class HabitWidgetConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as NeXApp
            val templateRepo = app.templateRepo
            val habitRepo = app.habitWidgetRepo

            val templates by templateRepo.observeTemplates()
                .collectAsState(initial = emptyList())

            var selectedTemplateId by remember { mutableStateOf<String?>(null) }
            var days by remember { mutableStateOf("21") }

            Column {
                Text("Select Template")
                templates.forEach {
                    RadioButton(
                        selected = selectedTemplateId == it.templateId,
                        onClick = { selectedTemplateId = it.templateId }
                    )
                    Text(it.name)
                }

                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("Days to track") }
                )

                val scope = rememberCoroutineScope()

                Button(onClick = {
                    scope.launch {
                        habitRepo.createWidget(
                            widgetId,
                            selectedTemplateId ?: return@launch,
                            days.toInt()
                        )

                        HabitWidgetUpdater.refreshWidget(this@HabitWidgetConfigureActivity, widgetId)

                        val result = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }

                        setResult(Activity.RESULT_OK, result)

                        finish()
                    }
                })
                {
                    Text("Create Habit Widget")
                }
            }
        }
    }
}
