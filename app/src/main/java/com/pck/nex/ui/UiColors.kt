package com.pck.nex.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class UiColors(
    val text: Color,
    val outline: Color,

    // Backgrounds for UI surfaces
    val panelFill: Color,    // for date chip, task rows, etc.
    val fieldFill: Color,    // behind the text field
    val buttonFill: Color,   // behind buttons

    val buttonText: Color
)

private fun Color.relLuminance(): Double {
    fun chan(c: Double): Double = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    val r = chan(this.red.toDouble())
    val g = chan(this.green.toDouble())
    val b = chan(this.blue.toDouble())
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun contrastRatio(a: Color, b: Color): Double {
    val l1 = a.relLuminance()
    val l2 = b.relLuminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Pick black/white text based on which contrasts more with base background color.
 * All UI translucent fills are derived from the chosen text color, so they "shift"
 * together (white-tinted panels on dark days, black-tinted panels on light days).
 */
fun uiColorsForBase(base: Color): UiColors {
    val black = Color(0xFF000000)
    val white = Color(0xFFFFFFFF)

    val cBlack = contrastRatio(black, base)
    val cWhite = contrastRatio(white, base)

    val text = if (cWhite >= cBlack) white else black

    return UiColors(
        text = text,
        outline = text,

        // Tuned alphas: panel is noticeable, field slightly stronger, buttons strongest.
        panelFill = text.copy(alpha = 0.28f),
        fieldFill = text.copy(alpha = 0.34f),
        buttonFill = text.copy(alpha = 0.42f),

        buttonText = text
    )
}
