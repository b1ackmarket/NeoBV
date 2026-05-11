package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean,
    segmentMarks: List<ProgressSegmentMark> = emptyList()
) {
    val colors: SliderColors = SliderDefaults.colors()
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp
    val segmentRanges = calculateProgressSegmentRanges(duration, segmentMarks)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackWidthDp)
            .clip(RoundedCornerShape(50))
    ) {
        val trackWidthPx = trackWidthDp.toPx()
        val currentFraction = if (duration > 0L) {
            position.coerceIn(0L, duration) / duration.toFloat()
        } else {
            0f
        }
        val bufferedFraction = (bufferedPercentage.coerceIn(0, 100) / 100f)

        drawLine(
            color = colors.inactiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = trackWidthPx,
            cap = StrokeCap.Round
        )
        if (!isPersistentSeek) {
            val bufferedEndX = size.width * bufferedFraction
            if (bufferedEndX > trackWidthPx / 2) {
                drawLine(
                    color = colors.disabledActiveTrackColor,
                    start = Offset(trackWidthPx / 2, center.y),
                    end = Offset(bufferedEndX, center.y),
                    strokeWidth = trackWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
        val activeEndX = size.width * currentFraction
        if (activeEndX > trackWidthPx / 2) {
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(trackWidthPx / 2, center.y),
                end = Offset(activeEndX, center.y),
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
                    start = Offset(startX, center.y),
                    end = Offset(endX, center.y),
                    strokeWidth = trackWidthPx,
                    cap = StrokeCap.Butt
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
