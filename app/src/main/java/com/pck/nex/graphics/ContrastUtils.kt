package com.pck.nex.graphics

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ContrastUtils {
    // Relative luminance per WCAG
    private fun Color.luminance(): Double {
        fun chan(c: Double): Double = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        val r = chan(this.red.toDouble())
        val g = chan(this.green.toDouble())
        val b = chan(this.blue.toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun worstContrastOnDual(fg: Color, a: Color, b: Color): Double {
        return minOf(contrastRatio(fg, a), contrastRatio(fg, b))
    }

    fun bestTextOnDualWithScore(a: Color, b: Color): Pair<Color, Double> {
        val black = Color(0xFF000000)
        val white = Color(0xFFFFFFFF)
        val worstBlack = worstContrastOnDual(black, a, b)
        val worstWhite = worstContrastOnDual(white, a, b)
        return if (worstBlack >= worstWhite) black to worstBlack else white to worstWhite
    }


    fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = fg.luminance()
        val l2 = bg.luminance()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun bestTextOnDual(colorA: Color, colorB: Color): Color {
        val black = Color(0xFF000000)
        val white = Color(0xFFFFFFFF)

        val worstBlack = minOf(contrastRatio(black, colorA), contrastRatio(black, colorB))
        val worstWhite = minOf(contrastRatio(white, colorA), contrastRatio(white, colorB))

        return if (worstBlack >= worstWhite) black else white
    }


    // Choose black or white text that meets AA best over a solid bg color
    fun bestTextOn(background: Color): Color {
        val black = Color(0xFF000000)
        val white = Color(0xFFFFFFFF)
        val cBlack = contrastRatio(black, background)
        val cWhite = contrastRatio(white, background)
        return if (cBlack >= cWhite) black else white
    }
}
