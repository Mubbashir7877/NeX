package com.pck.nex.graphics.debug

import android.graphics.Bitmap
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import com.pck.nex.graphics.NexBackground
import com.pck.nex.graphics.NexBackgroundCanvas
import com.pck.nex.graphics.PatternType
import com.pck.nex.graphics.buildBackground
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min

/**
 * Hardcoded dev switches.
 * Flip these while coding.
 */
object PatternCatalogConfig {
    const val ENABLE_EXPORT = false
    val TEST_SINGLE_PATTERN_ID: Int? = null // e.g. 11 for WAVES; null = all
    const val TILE_SIZE_PX = 360
    const val COLUMNS = 4
    const val SEED = 1234L
}

/**
 * Compose-based pattern catalog exporter (safe on Android).
 *
 * IMPORTANT:
 * Call from an Activity (ComponentActivity), ideally after setContent().
 */
object PatternCatalogExporter {

    fun runIfEnabled(activity: ComponentActivity) {
        if (!PatternCatalogConfig.ENABLE_EXPORT) return

        try {
            val ids = computePatternIds()
            val tile = PatternCatalogConfig.TILE_SIZE_PX
            val cols = PatternCatalogConfig.COLUMNS
            val rows = ceil(ids.size / cols.toFloat()).toInt().coerceAtLeast(1)

            val widthPx = cols * tile
            val heightPx = rows * tile

            // Create a hidden ComposeView attached to the window so it can obtain a recomposer.
            val composeView = ComposeView(activity).apply {
                visibility = View.INVISIBLE
                layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
                setContent {
                    CatalogGrid(
                        widthPx = widthPx,
                        heightPx = heightPx,
                        tilePx = tile,
                        columns = cols,
                        patternIds = ids
                    )
                }
            }

            // Attach to decor view (top-level window content)
            val root = activity.window.decorView as ViewGroup
            root.addView(composeView)

            // Wait until Compose has produced a frame, then capture.
            composeView.doOnPreDraw {
                try {
                    val bmp: Bitmap = composeView.drawToBitmap().copy(Bitmap.Config.ARGB_8888, false)
                    saveBitmap(activity, bmp)
                } catch (_: Throwable) {
                    // Never crash the app due to a debug exporter
                } finally {
                    // Always remove the debug view
                    root.removeView(composeView)
                }
            }
        } catch (_: Throwable) {
            // Never crash the app due to a debug exporter
        }
    }

    private fun computePatternIds(): List<Int> {
        val maxId = PatternType.entries.maxOf { it.id }
        val requested = PatternCatalogConfig.TEST_SINGLE_PATTERN_ID

        return if (requested != null) {
            listOf(requested.coerceIn(0, maxId))
        } else {
            PatternType.entries.map { it.id }.sorted()
        }
    }

    @Composable
    private fun CatalogGrid(
        widthPx: Int,
        heightPx: Int,
        tilePx: Int,
        columns: Int,
        patternIds: List<Int>
    ) {
        // We build the grid manually using offsets so each tile is exactly tilePx x tilePx.
        Box(modifier = Modifier.size(widthPx.dp, heightPx.dp)) {
            patternIds.forEachIndexed { index, id ->
                val col = index % columns
                val row = index / columns

                val bg: NexBackground = buildBackground(
                    seed = PatternCatalogConfig.SEED,
                    forcedTypeId = id
                )

                Box(
                    modifier = Modifier
                        .offset((col * tilePx).dp, (row * tilePx).dp)
                        .size(tilePx.dp, tilePx.dp)
                ) {
                    NexBackgroundCanvas(
                        modifier = Modifier.size(tilePx.dp, tilePx.dp),
                        background = bg
                    )
                }
            }
        }
    }

    private fun saveBitmap(activity: ComponentActivity, bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val fileName = "$timestamp.png"

        val dir = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "pattern_debug"
        )
        if (!dir.exists()) dir.mkdirs()

        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
