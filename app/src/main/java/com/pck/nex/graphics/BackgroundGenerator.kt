package com.pck.nex.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

val NEX_PALETTE: List<Color> = listOf(
    Color(0xFF111827), Color(0xFF1F2937), Color(0xFF0EA5E9), Color(0xFF22C55E),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF14B8A6),
    Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFF43F5E)
)

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

fun buildBackground(seed: Long, forcedTypeId: Int? = null): NexBackground {
    val rnd = Random(seed)
    val i1 = rnd.nextInt(NEX_PALETTE.size)
    var i2 = rnd.nextInt(NEX_PALETTE.size)
    if (i2 == i1) i2 = (i2 + 1) % NEX_PALETTE.size

    val type = forcedTypeId?.let { id ->
        PatternType.entries.firstOrNull { it.id == id } ?: PatternType.entries[rnd.nextInt(PatternType.entries.size)]
    } ?: PatternType.entries[rnd.nextInt(PatternType.entries.size)]

    return NexBackground(seed = seed, type = type, a = NEX_PALETTE[i1], b = NEX_PALETTE[i2])
}

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
            val cosA = cos(angleRad); val sinA = sin(angleRad)
            var t = -diag
            while (t < diag * 2f) {
                // Line along angle
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
            PatternType.DIAG_STRIPES -> stripe(step = min(w,h)/7f, angleRad = Math.PI.toFloat()/4f, thick = min(w,h)/18f)
            PatternType.WIDE_DIAG_STRIPES -> stripe(step = min(w,h)/4.5f, angleRad = Math.PI.toFloat()/4f, thick = min(w,h)/10f)
            PatternType.H_STRIPES -> {
                val step = h/10f; val thick = h/18f
                var y = 0f
                while (y < h) { drawRect(b, topLeft = Offset(0f,y), size = Size(w,thick)); y += step }
            }
            PatternType.V_STRIPES -> {
                val step = w/10f; val thick = w/18f
                var x = 0f
                while (x < w) { drawRect(b, topLeft = Offset(x,0f), size = Size(thick,h)); x += step }
            }
            PatternType.CHECKER -> {
                val cell = min(w,h)/10f
                var y=0f
                while (y < h) {
                    var x=0f
                    while (x < w) {
                        val on = (((x/cell).toInt() + (y/cell).toInt()) % 2 == 0)
                        if (on) drawRect(b, topLeft = Offset(x,y), size = Size(cell,cell))
                        x += cell
                    }
                    y += cell
                }
            }
            PatternType.DOTS_GRID -> {
                val step = min(w,h)/9f
                val r = step/7f
                var y = step/2f
                while (y < h) {
                    var x = step/2f
                    while (x < w) {
                        drawCircle(b, r, center = Offset(x,y))
                        x += step
                    }
                    y += step
                }
            }
            PatternType.DOTS_RANDOM -> {
                val n = 120
                repeat(n) {
                    val x = rnd.nextFloat() * w
                    val y = rnd.nextFloat() * h
                    val r = (min(w,h)/80f) + rnd.nextFloat()*(min(w,h)/60f)
                    drawCircle(b, r, center = Offset(x,y))
                }
            }
            PatternType.TRI_TILING -> {
                val s = min(w,h)/8f
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        val up = ((x/s).toInt() + (y/s).toInt()) % 2 == 0
                        val p1 = Offset(x, y)
                        val p2 = Offset(x + s, y)
                        val p3 = if (up) Offset(x + s/2f, y - s) else Offset(x + s/2f, y + s)
                        drawLine(b, p1, p2, strokeWidth = 4f)
                        drawLine(b, p2, p3, strokeWidth = 4f)
                        drawLine(b, p3, p1, strokeWidth = 4f)
                        x += s
                    }
                    y += s
                }
            }
            PatternType.DIAMOND_TILING -> {
                val s = min(w,h)/8f
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        val cx = x
                        val cy = y
                        drawLine(b, Offset(cx, cy - s/2f), Offset(cx + s/2f, cy), 5f)
                        drawLine(b, Offset(cx + s/2f, cy), Offset(cx, cy + s/2f), 5f)
                        drawLine(b, Offset(cx, cy + s/2f), Offset(cx - s/2f, cy), 5f)
                        drawLine(b, Offset(cx - s/2f, cy), Offset(cx, cy - s/2f), 5f)
                        x += s
                    }
                    y += s
                }
            }
            PatternType.CROSSHATCH -> {
                stripe(step = min(w,h)/6.5f, angleRad = Math.PI.toFloat()/4f, thick = min(w,h)/28f)
                stripe(step = min(w,h)/6.5f, angleRad = -Math.PI.toFloat()/4f, thick = min(w,h)/28f)
            }
            PatternType.ZIGZAG -> {
                val step = w/10f
                val amp = min(w,h)/25f
                var x=0f
                while (x < w + step) {
                    val yMid = h/2f + sin(x/step)*amp*3f
                    drawLine(b, Offset(x, yMid - amp), Offset(x + step/2f, yMid + amp), 6f)
                    drawLine(b, Offset(x + step/2f, yMid + amp), Offset(x + step, yMid - amp), 6f)
                    x += step
                }
            }
            PatternType.WAVES -> {
                val step = w/12f
                val amp = min(w,h)/18f
                var y = amp
                while (y < h) {
                    var x=0f
                    while (x < w) {
                        val y2 = y + sin((x/step) * 2f*PI.toFloat()) * amp
                        drawCircle(b, radius = 3.5f, center = Offset(x, y2))
                        x += 10f
                    }
                    y += amp*1.4f
                }
            }
            PatternType.CONCENTRIC_CIRCLES -> {
                val center = Offset(w/2f,h/2f)
                var r = min(w,h)/18f
                while (r < min(w,h)) {
                    drawCircle(b, r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    r += min(w,h)/10f
                }
            }
            PatternType.RINGS_OFFCENTER -> {
                val center = Offset(w*0.35f, h*0.4f)
                var r = min(w,h)/20f
                while (r < min(w,h)) {
                    drawCircle(b, r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
                    r += min(w,h)/9f
                }
            }
            PatternType.GRID_LINES -> {
                val step = min(w,h)/10f
                var x=0f; while (x < w) { drawLine(b, Offset(x,0f), Offset(x,h), 3f); x += step }
                var y=0f; while (y < h) { drawLine(b, Offset(0f,y), Offset(w,y), 3f); y += step }
            }
            PatternType.GRID_BLOCKS -> {
                val cell = min(w,h)/8f
                var y=0f
                while (y < h) {
                    var x=0f
                    while (x < w) {
                        if (rnd.nextFloat() > 0.55f) drawRect(b, topLeft = Offset(x,y), size = Size(cell,cell))
                        x += cell
                    }
                    y += cell
                }
            }
            PatternType.QUADS_RANDOM -> {
                repeat(60) {
                    val rw = rnd.nextFloat() * (w/3f) + 40f
                    val rh = rnd.nextFloat() * (h/3f) + 40f
                    val x = rnd.nextFloat() * (w - rw)
                    val y = rnd.nextFloat() * (h - rh)
                    drawRect(b.copy(alpha = 0.55f), topLeft = Offset(x,y), size = Size(rw,rh))
                }
            }
            PatternType.ARCS -> {
                val cx = w/2f; val cy = h/2f
                val rStep = min(w,h)/12f
                var r = rStep
                while (r < min(w,h)) {
                    drawCircle(b.copy(alpha = 0.35f), r, center = Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f))
                    r += rStep
                }
            }
            PatternType.SUNBURST -> {
                val center = Offset(w/2f,h/2f)
                val rays = 40
                val len = hypot(w.toDouble(),h.toDouble()).toFloat()
                for (i in 0 until rays) {
                    val ang = (i.toFloat()/rays) * 2f*PI.toFloat()
                    val end = Offset(center.x + cos(ang)*len, center.y + sin(ang)*len)
                    drawLine(b.copy(alpha = 0.6f), center, end, strokeWidth = 6f)
                }
            }
            PatternType.LOWPOLY_LITE -> {
                val step = min(w,h)/6f
                var y=0f
                while (y < h + step) {
                    var x=0f
                    while (x < w + step) {
                        val x2 = x + step
                        val y2 = y + step
                        val mid = Offset(x + step/2f + rnd.nextFloat()*20f, y + step/2f + rnd.nextFloat()*20f)
                        drawLine(b, Offset(x,y), mid, 4f)
                        drawLine(b, mid, Offset(x2,y), 4f)
                        drawLine(b, mid, Offset(x,y2), 4f)
                        drawLine(b, mid, Offset(x2,y2), 4f)
                        x += step
                    }
                    y += step
                }
            }
        }
    }
}
