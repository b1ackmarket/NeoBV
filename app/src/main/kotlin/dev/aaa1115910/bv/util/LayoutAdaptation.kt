package dev.aaa1115910.bv.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.max
import kotlin.math.min

internal fun resolveDefaultDensity(widthPx: Int, heightPx: Int): Float {
    val shortSide = min(widthPx, heightPx).coerceAtLeast(1)
    val longSide = max(widthPx, heightPx).coerceAtLeast(1)
    val base = if (shortSide < 900 && longSide >= 1800) {
        longSide / 1440f
    } else {
        longSide / 960f
    }
    return (base * 10).toInt().div(10f).coerceIn(0.8f, 2.2f)
}

internal fun resolveAdaptiveGridMinCellWidth(defaultColumns: Int): Int {
    return when {
        defaultColumns >= 6 -> 150
        defaultColumns == 5 -> 170
        defaultColumns == 3 -> 250
        else -> 220
    }
}

@Composable
fun rememberAdaptiveGridCells(defaultColumns: Int): GridCells {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val minCellWidth = resolveAdaptiveGridMinCellWidth(defaultColumns)
    val columns = (widthDp / minCellWidth).toInt()
        .coerceIn(1, defaultColumns.coerceAtLeast(1))
    return GridCells.Fixed(columns)
}
