package com.pck.nex.widget

import android.graphics.Color
import kotlin.math.abs

object WidgetPalette {

    // Stable, pleasant matte background color derived from seed/type.
    // (Widgets cannot render your generative canvas; this preserves "day identity".)
    fun bgFromSeed(seed: Long, type: Int): Int {
        val x = seed xor (type.toLong() shl 32)
        val r = 210 - (abs((x       ).toInt()) % 70) // 140..210
        val g = 210 - (abs((x shr 8 ).toInt()) % 70)
        val b = 210 - (abs((x shr 16).toInt()) % 70)
        return Color.rgb(r.coerceIn(120, 230), g.coerceIn(120, 230), b.coerceIn(120, 230))
    }
}
