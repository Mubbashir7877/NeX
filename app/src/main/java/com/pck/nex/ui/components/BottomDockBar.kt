package com.pck.nex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pck.nex.ui.UiColors

enum class DockTab { LIBRARY, ARCHIVE, TEMPLATE }

@Composable
fun BottomDockBar(
    selected: DockTab,
    ui: UiColors,
    onLibrary: () -> Unit,
    onArchive: () -> Unit,
    onTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 520.dp)
            .height(56.dp)
            .background(ui.panelFill, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton("Library", selected == DockTab.LIBRARY, ui, onLibrary)
        DockButton("Archive", selected == DockTab.ARCHIVE, ui, onArchive)
        DockButton("Template", selected == DockTab.TEMPLATE, ui, onTemplate)
    }
}

@Composable
private fun DockButton(
    label: String,
    selected: Boolean,
    ui: UiColors,
    onClick: () -> Unit
) {
    val fill = if (selected) ui.buttonFill.copy(alpha = 0.95f) else ui.buttonFill.copy(alpha = 0.55f)
    val textColor = if (selected) ui.buttonText else ui.buttonText.copy(alpha = 0.85f)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(label, color = textColor)
    }
}
