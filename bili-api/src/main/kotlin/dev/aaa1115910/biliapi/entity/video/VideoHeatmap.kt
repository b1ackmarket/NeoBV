package dev.aaa1115910.biliapi.entity.video

import dev.aaa1115910.biliapi.http.entity.video.VideoPbp

data class VideoHeatmapPoint(
    val startMs: Long,
    val endMs: Long,
    val value: Double
)

data class VideoHeatmap(
    val stepMs: Long,
    val points: List<VideoHeatmapPoint>
) {
    companion object {
        fun fromPbp(pbp: VideoPbp): VideoHeatmap? {
            val stepMs = pbp.stepSec.takeIf { it > 0 }?.toLong()?.times(1000L) ?: return null
            val values = pbp.events?.default.orEmpty()
            if (values.isEmpty()) return null

            return VideoHeatmap(
                stepMs = stepMs,
                points = values.mapIndexedNotNull { index, value ->
                    if (!value.isFinite() || value < 0.0) return@mapIndexedNotNull null
                    val startMs = index * stepMs
                    VideoHeatmapPoint(
                        startMs = startMs,
                        endMs = startMs + stepMs,
                        value = value
                    )
                }
            ).takeIf { it.points.isNotEmpty() }
        }
    }
}
