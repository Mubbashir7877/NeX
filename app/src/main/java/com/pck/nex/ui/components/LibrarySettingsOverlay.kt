package com.pck.nex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pck.nex.ui.UiColors

@Composable
fun LibrarySettingsOverlay(
    ui: UiColors,
    onPickBackground: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ FIX: remember the interaction source
    val dismissInteractionSource = remember { MutableInteractionSource() }

    // Full-screen click catcher behind the panel
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = dismissInteractionSource
            ) {
                onDismiss()
            }
    ) {
        Surface(
            color = ui.panelFill,
            shape = RoundedCornerShape(
                bottomStart = 22.dp,
                bottomEnd = 22.dp
            ),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Library Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = ui.text
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Library Background",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ui.text.copy(alpha = 0.8f)
                )

                Button(
                    onClick = onPickBackground,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ui.buttonFill,
                        contentColor = ui.buttonText
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Choose image")
                }
            }
        }
    }
}
