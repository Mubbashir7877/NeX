package com.pck.nex.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

/**
 * Key design constraints implemented here:
 * 1) Two-color backgrounds should be harmonious, not harsh opposites.
 *    - Avoid near-black + near-white pairings.
 *    - Prefer "neighbors" in luminance (e.g., dark + slightly-less-dark).
 * 2) Background base color must still allow a clean third color (black or white) for text/UI.
 *    - Ensure either black OR white contrasts strongly with the base.
 *
 * We treat background as:
 *   a = base fill
 *   b = pattern ink
 */

enum class PatternType(val id: Int) {
    DIAG_STRIPES(0),
    WIDE_DIAG_STRIPES(1),
    H_STRIPES(2),
    V_STRIPES(3),
    CHECKER(4),
    DOTS_GRID(5),
    DOTS_RANDOM(6),
    TRI_TILING(7),
    DIAMOND_TILING(8),
    CROSSHATCH(9),
    ZIGZAG(10),
    WAVES(11),
    CONCENTRIC_CIRCLES(12),
    RINGS_OFFCENTER(13),
    GRID_LINES(14),
    GRID_BLOCKS(15),
    QUADS_RANDOM(16),
    ARCS(17),
    SUNBURST(18),
    LOWPOLY_LITE(19),
}

data class NexBackground(
    val seed: Long,
    val type: PatternType,
    val a: Color,
    val b: Color
)

/**
 * Balanced palette, but not used as "random free-for-all".
 * We choose pairs via luminance neighborhood rules below.
 *
 * These are "good looking" colors, not neon. Includes a few light neutrals,
 * but we intentionally avoid pairing extremes together.
 */
private val NEX_PALETTE: List<Color> = listOf(
    // Dark neutrals
    Color(0xFF0B1220),
    Color(0xFF111827),
    Color(0xFF1F2937),

    // Dark chroma (still subdued)
    Color(0xFF1E3A8A), // deep indigo
    Color(0xFF0F766E), // deep teal
    Color(0xFF166534), // deep green
    Color(0xFF6FA8A1),
    Color(0xFF8A6C01 ),
    Color(0xFF100B36),
    Color(0xFF36072F),
    Color(0xFF280B36),
    Color(0XFF4D1702), // deep orange-brown
    Color(0xFF7F1D1D), // deep red

    // Mid tones (muted)
    Color(0xFFF520D5),
    Color(0xFF1D4ED8), // blue-700
    Color(0xFF0284C7), // sky-600
    Color(0xFF0D9488), // teal-600
    Color(0xFF16A34A), // green-600
    Color(0xFF1CB302 ),
    Color(0xFFF5E642),
    Color(0xFFD97706), // amber-600
    Color(0xFFDC2626), // red-600
    Color(0xFF7C3AED), // violet-600

    // Light neutrals (used carefully)
    Color(0xFFE5E7EB),
    Color(0xFFFC9292 ),
    Color(0xFFFCB092 ),
    Color(0xFFC1F5B8 ),
    Color(0xFFF7EF8F ),
    Color(0xFFA1FFFC ),
    Color(0xFFFFA3EA),
    Color(0xFFA9ADFC ),
    Color(0xFFFFF7ED)
)

fun buildBackground(seed: Long, forcedTypeId: Int? = null): NexBackground {
    val rnd = Random(seed)

    val type = forcedTypeId?.let { id ->
        PatternType.entries.firstOrNull { it.id == id } ?: PatternType.entries[rnd.nextInt(PatternType.entries.size)]
    } ?: PatternType.entries[rnd.nextInt(PatternType.entries.size)]

    val (a, b) = pickHarmoniousPair(seed)

    return NexBackground(
        seed = seed,
        type = type,
        a = a,
        b = b
    )
}

/**
 * Picks two colors that:
 * - are not extreme opposites in luminance
 * - are still distinct enough to show a pattern
 * - allow black or white text to remain clearly readable
 */
private fun pickHarmoniousPair(seed: Long): Pair<Color, Color> {
    val rnd = Random(seed)

    // 1) Choose a base color that yields a clear UI text color (black or white)
    // Require contrast >= ~4.5 against either black or white (using base only).
    val candidates = NEX_PALETTE.shuffled(rnd).filter { base ->
        val cb = contrastRatio(Color.Black, base)
        val cw = contrastRatio(Color.White, base)
        max(cb, cw) >= 4.5
    }.ifEmpty {
        // Fallback: use whole palette if filtered too hard (should be rare)
        NEX_PALETTE.shuffled(rnd)
    }

    val base = candidates.first()

    // 2) Choose accent close in luminance to base (not opposite)
    // We want "black + charcoal" not "black + white", etc.
    val baseLum = relLuminance(base)

    // Luminance neighborhood:
    // - minimum difference to be visible
    // - maximum difference to avoid harsh contrast
    val minDelta = 0.04
    val maxDelta = 0.22

    val accentPool = NEX_PALETTE
        .filter { it != base }
        .map { it to abs(relLuminance(it) - baseLum) }
        .filter { (_, d) -> d in minDelta..maxDelta }
        .sortedBy { (_, d) -> d } // closer first: more harmonious
        .map { it.first }

    val accent = if (accentPool.isNotEmpty()) {
        // Pick among the closest few to add variety but stay harmonious
        val top = accentPool.take(min(6, accentPool.size))
        top[rnd.nextInt(top.size)]
    } else {
        // If we can't find within range, pick the closest by luminance anyway
        NEX_PALETTE
            .filter { it != base }
            .minBy { abs(relLuminance(it) - baseLum) }
    }

    // 3) Ensure we didn't accidentally choose extreme opposite (safety net)
    val lumDelta = abs(relLuminance(accent) - baseLum)
    if (lumDelta > 0.35) {
        // Force to closest if something went wrong (should not happen with the above)
        val closest = NEX_PALETTE
            .filter { it != base }
            .minBy { abs(relLuminance(it) - baseLum) }
        return base to closest
    }

    return base to accent
}

/* ---------- Contrast helpers (local, simple, reliable) ---------- */

private fun relLuminance(c: Color): Double {
    fun chan(x: Double): Double = if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
    val r = chan(c.red.toDouble())
    val g = chan(c.green.toDouble())
    val b = chan(c.blue.toDouble())
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relLuminance(a)
    val l2 = relLuminance(b)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

/* ---------- Pattern renderer (20 patterns) ---------- */

@Composable
fun NexBackgroundCanvas(modifier: Modifier, background: NexBackground) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val rnd = Random(background.seed)

        val a = background.a
        val b = background.b

        // Base fill
        drawRect(color = a)

        fun stripe(step: Float, angleRad: Float, thick: Float) {
            val diag = hypot(w.toDouble(), h.toDouble()).toFloat()
            val cosA = cos(angleRad)
            val sinA = sin(angleRad)
            var t = -diag
            while (t < diag * 2f) {
                val x0 = t * cosA
                val y0 = t * sinA
                drawLine(
                    color = b,
                    start = Offset(x0, y0),
                    end = Offset(x0 + diag * cosA - diag * sinA, y0 + diag * sinA + diag * cosA),
                    strokeWidth = thick
                )
                t += step
            }
        }

        when (background.type) {
            PatternType.DIAG_STRIPES ->
                stripe(step = min(w, h) / 7f, angleRad = PI.toFloat() / 19f, thick = min(w, h) / 24f)

            PatternType.WIDE_DIAG_STRIPES ->
                stripe(step = min(w, h) / 4.5f, angleRad = PI.toFloat() / -9f, thick = min(w, h) / 30f)

            PatternType.H_STRIPES -> {
                val step = h / 7f
                val thick = h / 38f
                var y = 0f
                while (y < h) {
                    drawRect(b, topLeft = Offset(0f, y), size = Size(w, thick))
                    y += step
                }
            }

            PatternType.V_STRIPES -> {
                val step = w / 10f
                val thick = w / 28f
                var x = 0f
                while (x < w) {
                    drawRect(b, topLeft = Offset(x, 0f), size = Size(thick, h))
                    x += step
                }
            }

            PatternType.CHECKER -> {
                val cell = min(w, h) / 10f
                var y = 0f
                while (y < h) {
                    var x = 0f
                    while (x < w) {
                        val on = (((x / cell).toInt() + (y / cell).toInt()) % 2 == 0)
                        if (on) drawRect(b, topLeft = Offset(x, y), size = Size(cell, cell))
                        x += cell
                    }
                    y += cell
                }
            }

            PatternType.DOTS_GRID -> {
                val step = min(w, h) / 9f
                val r = step / 7f
                var y = step / 2f
                while (y < h) {
                    var x = step / 2f
                    while (x < w) {
                        drawCircle(b, r, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }

            PatternType.DOTS_RANDOM -> {
                repeat(120) {
                    val x = rnd.nextFloat() * w
                    val y = rnd.nextFloat() * h
                    val r = (min(w, h) / 90f) + rnd.nextFloat() * (min(w, h) / 70f)
                    drawCircle(b, r, center = Offset(x, y))
                }
            }

            PatternType.TRI_TILING -> {
                val s = min(w, h) / 8f
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        val up = ((x / s).toInt() + (y / s).toInt()) % 2 == 0
                        val p1 = Offset(x, y)
                        val p2 = Offset(x + s, y)
                        val p3 = if (up) Offset(x + s / 2f, y - s) else Offset(x + s / 2f, y + s)
                        drawLine(b, p1, p2, strokeWidth = 4f)
                        drawLine(b, p2, p3, strokeWidth = 4f)
                        drawLine(b, p3, p1, strokeWidth = 4f)
                        x += s
                    }
                    y += s
                }
            }

            PatternType.DIAMOND_TILING -> {
                val s = min(w, h) / 8f
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        val cx = x
                        val cy = y
                        drawLine(b, Offset(cx, cy - s / 2f), Offset(cx + s / 2f, cy), 5f)
                        drawLine(b, Offset(cx + s / 2f, cy), Offset(cx, cy + s / 2f), 5f)
                        drawLine(b, Offset(cx, cy + s / 2f), Offset(cx - s / 2f, cy), 5f)
                        drawLine(b, Offset(cx - s / 2f, cy), Offset(cx, cy - s / 2f), 5f)
                        x += s
                    }
                    y += s
                }
            }

            PatternType.CROSSHATCH -> {
                stripe(step = min(w, h) / 6.5f, angleRad = PI.toFloat() / 4f, thick = min(w, h) / 28f)
                stripe(step = min(w, h) / 6.5f, angleRad = -PI.toFloat() / 4f, thick = min(w, h) / 28f)
            }

            PatternType.ZIGZAG -> {
                val s = min(w, h) / 13f
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        val up = ((x / s).toInt() + (y / s).toInt()) % 2 == 0
                        val p1 = Offset(x, y)
                        val p2 = Offset(x + s, y)
                        val p3 = if (up) Offset(x + s / 2f, y - s) else Offset(x + s / 2f, y + s)
                        drawLine(b, p1, p2, strokeWidth = 4f)
                        drawLine(b, p2, p3, strokeWidth = 4f)
                        drawLine(b, p3, p1, strokeWidth = 4f)
                        x += s
                    }
                    y += s
                }
            }

            PatternType.WAVES -> {
                val step = w / 12f
                val amp = min(w, h) / 16f
                var y = amp
                while (y < h) {
                    var x = 0f
                    while (x < w) {
                        val y2 = y + sin((x / step) * 2f * PI.toFloat()) * amp
                        drawCircle(b, radius = 3.2f, center = Offset(x, y2))
                        x += 6f
                    }
                    y += amp * 1.4f
                }
            }

            PatternType.CONCENTRIC_CIRCLES -> {
                val center = Offset(w / 2f, h / 2f)
                var r = min(w, h) / 18f
                while (r < min(w, h)) {
                    drawCircle(b, r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    r += min(w, h) / 10f
                }
            }

            PatternType.RINGS_OFFCENTER -> {
                val center = Offset(w * 0.35f, h * 0.4f)
                var r = min(w, h) / 20f
                while (r < min(w, h)) {
                    drawCircle(b, r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
                    r += min(w, h) / 9f
                }
            }

            PatternType.GRID_LINES -> {
                val step = min(w, h) / 10f
                var x = 0f
                while (x < w) {
                    drawLine(b, Offset(x, 0f), Offset(x, h), 3f)
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(b, Offset(0f, y), Offset(w, y), 3f)
                    y += step
                }
            }

            PatternType.GRID_BLOCKS -> {
                val cell = min(w, h) / 8f
                var y = 0f
                while (y < h) {
                    var x = 0f
                    while (x < w) {
                        if (rnd.nextFloat() > 0.55f) drawRect(b, topLeft = Offset(x, y), size = Size(cell, cell))
                        x += cell
                    }
                    y += cell
                }
            }

            PatternType.QUADS_RANDOM -> {
                repeat(60) {
                    val rw = rnd.nextFloat() * (w / 3f) + 40f
                    val rh = rnd.nextFloat() * (h / 3f) + 40f
                    val x = rnd.nextFloat() * (w - rw)
                    val y = rnd.nextFloat() * (h - rh)
                    drawRect(b.copy(alpha = 0.55f), topLeft = Offset(x, y), size = Size(rw, rh))
                }
            }

            PatternType.ARCS -> {
                val center = Offset(w / 2f, h / 2f)
                val rStep = min(w, h) / 12f
                var r = rStep
                while (r < min(w, h)) {
                    drawCircle(
                        b.copy(alpha = 0.35f),
                        r,
                        center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f)
                    )
                    r += rStep
                }
            }

            PatternType.SUNBURST -> {
                val center = Offset(w / 2f, h / 2f)
                val rays = 40
                val len = hypot(w.toDouble(), h.toDouble()).toFloat()
                for (i in 0 until rays) {
                    val ang = (i.toFloat() / rays) * 2f * PI.toFloat()
                    val end = Offset(center.x + cos(ang) * len, center.y + sin(ang) * len)
                    drawLine(b.copy(alpha = 0.6f), center, end, strokeWidth = 6f)
                }
            }

            PatternType.LOWPOLY_LITE -> {
                val step = min(w, h) / 6f
                var y = 0f
                while (y < h + step) {
                    var x = 0f
                    while (x < w + step) {
                        val x2 = x + step
                        val y2 = y + step
                        val mid = Offset(
                            x + step / 2f + rnd.nextFloat() * 20f,
                            y + step / 2f + rnd.nextFloat() * 20f
                        )
                        drawLine(b, Offset(x, y), mid, 4f)
                        drawLine(b, mid, Offset(x2, y), 4f)
                        drawLine(b, mid, Offset(x, y2), 4f)
                        drawLine(b, mid, Offset(x2, y2), 4f)
                        x += step
                    }
                    y += step
                }
            }
        }
    }
}

fun patternCount(): Int = PatternType.entries.size
