package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.video.VideoHeatmap
import dev.aaa1115910.biliapi.entity.video.VideoHeatmapPoint
import dev.aaa1115910.biliapi.entity.video.VideoProgressChapter
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import dev.aaa1115910.bv.ui.theme.BVTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean,
    segmentMarks: List<ProgressSegmentMark> = emptyList(),
    watchedSegmentMarks: List<ProgressSegmentMark> = emptyList(),
    videoHeatmap: VideoHeatmap? = null,
    chapters: List<VideoProgressChapter> = emptyList()
) {
    val colors: SliderColors = SliderDefaults.colors()
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp
    val segmentRanges = calculateProgressSegmentRanges(duration, segmentMarks)
    val watchedRanges = calculateProgressSegmentRanges(duration, watchedSegmentMarks)
    val chapterTrackRanges = calculateChapterTrackRanges(duration, chapters)
    val hasChapterTrack = duration > 0L && chapters.isNotEmpty()
    val heatmapPoints = if (!isPersistentSeek) videoHeatmap?.points.orEmpty() else emptyList()
    val hasHeatmap = heatmapPoints.any { it.value > 0.0 }
    val canvasHeightDp = when {
        isPersistentSeek -> trackWidthDp
        hasHeatmap -> 42.dp
        else -> 24.dp
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp)
    ) {
        val trackWidthPx = trackWidthDp.toPx()
        val currentFraction = if (duration > 0L) {
            position.coerceIn(0L, duration) / duration.toFloat()
        } else {
            0f
        }
        val bufferedFraction = (bufferedPercentage.coerceIn(0, 100) / 100f)
        val trackCenterY = if (hasHeatmap) {
            size.height - trackWidthPx / 2f - 2.dp.toPx()
        } else {
            size.height - trackWidthPx / 2f - 3.dp.toPx()
        }
        val chapterGapPx = if (isPersistentSeek) 2.dp.toPx() else 4.dp.toPx()

        if (hasHeatmap && duration > 0L) {
            drawVideoHeatmap(
                points = heatmapPoints,
                duration = duration,
                watchedRanges = watchedRanges,
                playedColor = colors.activeTrackColor,
                unplayedColor = colors.inactiveTrackColor
            )
        }

        if (hasChapterTrack) {
            drawSegmentedProgressLine(
                trackRanges = chapterTrackRanges,
                color = colors.inactiveTrackColor,
                centerY = trackCenterY,
                strokeWidth = trackWidthPx,
                gapPx = chapterGapPx
            )
            if (!isPersistentSeek) {
                drawSegmentedProgressLine(
                    trackRanges = chapterTrackRanges,
                    color = colors.disabledActiveTrackColor,
                    centerY = trackCenterY,
                    strokeWidth = trackWidthPx,
                    gapPx = chapterGapPx,
                    endFraction = bufferedFraction
                )
            }
            drawSegmentedProgressLine(
                trackRanges = chapterTrackRanges,
                color = colors.activeTrackColor,
                centerY = trackCenterY,
                strokeWidth = trackWidthPx,
                gapPx = chapterGapPx,
                endFraction = currentFraction
            )
            segmentRanges.forEach { segment ->
                drawSegmentedProgressLine(
                    trackRanges = chapterTrackRanges,
                    color = Color(segment.colorArgb),
                    centerY = trackCenterY,
                    strokeWidth = trackWidthPx,
                    gapPx = chapterGapPx,
                    startFraction = segment.startFraction,
                    endFraction = segment.endFraction,
                    cap = StrokeCap.Butt
                )
            }
        } else {
            drawLine(
                color = colors.inactiveTrackColor,
                start = Offset(0f, trackCenterY),
                end = Offset(size.width, trackCenterY),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )
            if (!isPersistentSeek) {
                val bufferedEndX = size.width * bufferedFraction
                if (bufferedEndX > trackWidthPx / 2) {
                    drawLine(
                        color = colors.disabledActiveTrackColor,
                        start = Offset(trackWidthPx / 2, trackCenterY),
                        end = Offset(bufferedEndX, trackCenterY),
                        strokeWidth = trackWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }
            val activeEndX = size.width * currentFraction
            if (activeEndX > trackWidthPx / 2) {
                drawLine(
                    color = colors.activeTrackColor,
                    start = Offset(trackWidthPx / 2, trackCenterY),
                    end = Offset(activeEndX, trackCenterY),
                    strokeWidth = trackWidthPx,
                    cap = StrokeCap.Round
                )
            }
            segmentRanges.forEach { segment ->
                val startX = size.width * segment.startFraction
                val endX = size.width * segment.endFraction
                if (endX > startX) {
                    drawLine(
                        color = Color(segment.colorArgb),
                        start = Offset(startX, trackCenterY),
                        end = Offset(endX, trackCenterY),
                        strokeWidth = trackWidthPx,
                        cap = StrokeCap.Butt
                    )
                }
            }
        }
        if (!isPersistentSeek && duration > 0L) {
            drawCurrentPositionIndicator(
                fraction = currentFraction,
                centerY = trackCenterY,
                color = colors.activeTrackColor
            )
        }
    }

}

private fun DrawScope.drawCurrentPositionIndicator(
    fraction: Float,
    centerY: Float,
    color: Color
) {
    val x = (size.width * fraction.coerceIn(0f, 1f))
        .coerceIn(14.dp.toPx(), size.width - 14.dp.toPx())
    val bodyWidth = 18.dp.toPx()
    val bodyHeight = 12.dp.toPx()
    val antennaHeight = 3.dp.toPx()
    val bodyTop = (centerY - bodyHeight - 2.5.dp.toPx()).coerceAtLeast(antennaHeight + 1.dp.toPx())
    val bodyLeft = x - bodyWidth / 2f
    val screenWidth = 12.dp.toPx()
    val screenHeight = 6.5.dp.toPx()
    val screenLeft = x - screenWidth / 2f
    val screenTop = bodyTop + 2.6.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft, bodyTop),
        size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(screenLeft, screenTop),
        size = androidx.compose.ui.geometry.Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawLine(
        color = color,
        start = Offset(x - 4.5.dp.toPx(), bodyTop - antennaHeight),
        end = Offset(x - 1.4.dp.toPx(), bodyTop),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(x + 4.5.dp.toPx(), bodyTop - antennaHeight),
        end = Offset(x + 1.4.dp.toPx(), bodyTop),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = 0.8.dp.toPx(),
        center = Offset(x - 3.dp.toPx(), bodyTop + bodyHeight / 2f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = 0.8.dp.toPx(),
        center = Offset(x + 3.dp.toPx(), bodyTop + bodyHeight / 2f)
    )
    val pointer = Path().apply {
        moveTo(x, centerY - 1.dp.toPx())
        lineTo(x - 4.dp.toPx(), centerY - 7.dp.toPx())
        lineTo(x + 4.dp.toPx(), centerY - 7.dp.toPx())
        close()
    }
    drawPath(path = pointer, color = color)
}

private fun DrawScope.drawSegmentedProgressLine(
    trackRanges: List<ProgressTrackRange>,
    color: Color,
    centerY: Float,
    strokeWidth: Float,
    gapPx: Float,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    cap: StrokeCap = StrokeCap.Round
) {
    val safeStartFraction = startFraction.coerceIn(0f, 1f)
    val safeEndFraction = endFraction.coerceIn(0f, 1f)
    if (safeEndFraction <= safeStartFraction) return

    trackRanges.forEach { range ->
        val segmentStart = max(range.startFraction, safeStartFraction)
        val segmentEnd = min(range.endFraction, safeEndFraction)
        if (segmentEnd <= segmentStart) return@forEach

        val startsAtChapterEdge = abs(segmentStart - range.startFraction) < 0.0001f
        val endsAtChapterEdge = abs(segmentEnd - range.endFraction) < 0.0001f
        val startInset = when {
            !startsAtChapterEdge -> 0f
            range.startFraction <= 0f -> strokeWidth / 2f
            else -> gapPx / 2f
        }
        val endInset = when {
            !endsAtChapterEdge -> 0f
            range.endFraction >= 1f -> strokeWidth / 2f
            else -> gapPx / 2f
        }
        val startX = size.width * segmentStart + startInset
        val endX = size.width * segmentEnd - endInset
        if (endX <= startX) return@forEach

        drawLine(
            color = color,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = strokeWidth,
            cap = cap
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVideoHeatmap(
    points: List<VideoHeatmapPoint>,
    duration: Long,
    watchedRanges: List<ProgressSegmentRange>,
    playedColor: Color,
    unplayedColor: Color
) {
    val validPoints = points.filter { it.value > 0.0 && it.startMs < duration }
    if (validPoints.isEmpty()) return

    val maxValue = validPoints.maxOf { it.value }.takeIf { it > 0.0 } ?: return
    val heatmapTop = 2.dp.toPx()
    val heatmapBottom = size.height - 10.dp.toPx()
    val heatmapHeight = (heatmapBottom - heatmapTop).coerceAtLeast(1f)
    val areaPath = Path().apply {
        moveTo(0f, heatmapBottom)
        validPoints.forEach { point ->
            val centerMs = ((point.startMs + point.endMs) / 2L).coerceIn(0L, duration)
            val x = size.width * (centerMs / duration.toFloat())
            val normalized = sqrt((point.value / maxValue).coerceIn(0.0, 1.0)).toFloat()
            val y = heatmapBottom - normalized * heatmapHeight
            lineTo(x, y)
        }
        lineTo(size.width, heatmapBottom)
        close()
    }
    drawPath(
        path = areaPath,
        color = unplayedColor.copy(alpha = 0.30f)
    )
    watchedRanges.forEach { range ->
        val left = size.width * range.startFraction
        val right = size.width * range.endFraction
        if (right > left) {
            clipRect(left = left, right = right) {
                drawPath(
                    path = areaPath,
                    color = playedColor.copy(alpha = 0.48f)
                )
            }
        }
    }
}

internal data class ProgressSegmentRange(
    val startFraction: Float,
    val endFraction: Float,
    val colorArgb: Long
)

internal data class ProgressTrackRange(
    val startFraction: Float,
    val endFraction: Float
)

internal fun calculateProgressSegmentRanges(
    duration: Long,
    segments: List<ProgressSegmentMark>
): List<ProgressSegmentRange> {
    if (duration <= 0L) return emptyList()

    return segments.mapNotNull { segment ->
        val start = segment.startMs.coerceIn(0L, duration)
        val end = segment.endMs.coerceIn(0L, duration)
        if (end <= start) return@mapNotNull null
        ProgressSegmentRange(
            startFraction = start / duration.toFloat(),
            endFraction = end / duration.toFloat(),
            colorArgb = segment.colorArgb
        )
    }
}

internal fun calculateChapterMarkerFractions(
    duration: Long,
    chapters: List<VideoProgressChapter>
): List<Float> {
    if (duration <= 0L) return emptyList()
    return chapters
        .asSequence()
        .map { it.startMs }
        .filter { it in 1 until duration }
        .distinct()
        .sorted()
        .map { it / duration.toFloat() }
        .toList()
}

internal fun calculateChapterTrackRanges(
    duration: Long,
    chapters: List<VideoProgressChapter>
): List<ProgressTrackRange> {
    if (duration <= 0L) return emptyList()
    if (chapters.isEmpty()) return listOf(ProgressTrackRange(0f, 1f))

    val normalized = chapters
        .mapNotNull { chapter ->
            val start = chapter.startMs.coerceIn(0L, duration)
            val end = chapter.endMs.coerceIn(0L, duration)
            if (end <= start) return@mapNotNull null
            start to end
        }
        .sortedBy { it.first }

    if (normalized.isEmpty()) return listOf(ProgressTrackRange(0f, 1f))

    val ranges = mutableListOf<ProgressTrackRange>()
    var cursor = 0L
    normalized.forEach { (rawStart, rawEnd) ->
        if (rawStart > cursor) {
            ranges += ProgressTrackRange(
                startFraction = cursor / duration.toFloat(),
                endFraction = rawStart / duration.toFloat()
            )
        }
        val start = rawStart.coerceAtLeast(cursor)
        val end = rawEnd.coerceAtLeast(start)
        if (end > start) {
            ranges += ProgressTrackRange(
                startFraction = start / duration.toFloat(),
                endFraction = end / duration.toFloat()
            )
            cursor = end
        }
    }
    if (cursor < duration) {
        ranges += ProgressTrackRange(
            startFraction = cursor / duration.toFloat(),
            endFraction = 1f
        )
    }

    return ranges
}

internal fun calculateSegmentIntersections(
    trackRanges: List<ProgressTrackRange>,
    segmentRange: ProgressSegmentRange
): List<ProgressSegmentRange> {
    return trackRanges.mapNotNull { trackRange ->
        val start = max(trackRange.startFraction, segmentRange.startFraction)
        val end = min(trackRange.endFraction, segmentRange.endFraction)
        if (end <= start) return@mapNotNull null
        ProgressSegmentRange(
            startFraction = start,
            endFraction = end,
            colorArgb = segmentRange.colorArgb
        )
    }
}


@Preview(device = "id:tv_1080p")
@Composable
private fun SeekPreview() {
    BVTheme {
        VideoProgressSeek(
            duration = 1000,
            position = 300,
            bufferedPercentage = 50,
            isPersistentSeek = true
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SeekWithThumbPreview(@PreviewParameter(ProgressProvider::class) data: Triple<Long, Long, Int>) {
    BVTheme {
        VideoProgressSeek(
            duration = data.first,
            position = data.second,
            bufferedPercentage = data.third,
            isPersistentSeek = false
        )
    }
}

private class ProgressProvider : PreviewParameterProvider<Triple<Long, Long, Int>> {
    override val values = sequenceOf(
        Triple(1234_000L, 0L, 3),
        Triple(1234_000L, 234_000L, 24),
        Triple(1234_000L, 555_000L, 57),
        Triple(1234_000L, 1234_000L, 100)
    )
}
