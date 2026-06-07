package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.VideoProgressChapter
import dev.aaa1115910.biliapi.entity.video.currentChapterAt
import dev.aaa1115910.bv.entity.ProgressSegmentMark

@Composable
fun VideoChapterImmersiveBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    chapters: List<VideoProgressChapter>,
    sponsorBlockProgressMarks: List<ProgressSegmentMark> = emptyList(),
    watchedProgressMarks: List<ProgressSegmentMark> = emptyList()
) {
    val visibleChapters = remember(duration, chapters) {
        if (duration <= 0L) return@remember emptyList()
        chapters.mapNotNull { chapter ->
            val start = chapter.startMs.coerceIn(0L, duration)
            val end = chapter.endMs.coerceIn(0L, duration)
            if (end <= start || chapter.title.isBlank()) return@mapNotNull null
            chapter.copy(startMs = start, endMs = end)
        }
    }
    if (visibleChapters.isEmpty()) return

    val currentChapter = remember(visibleChapters, position) {
        visibleChapters.currentChapterAt(position)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.56f)
                    )
                )
            )
    ) {
        VideoProgressSeek(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            duration = duration,
            position = position,
            bufferedPercentage = bufferedPercentage,
            isPersistentSeek = true,
            segmentMarks = sponsorBlockProgressMarks,
            watchedSegmentMarks = watchedProgressMarks,
            chapters = visibleChapters
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleChapters.forEach { chapter ->
                val selected = currentChapter != null &&
                    chapter.startMs == currentChapter.startMs &&
                    chapter.endMs == currentChapter.endMs
                Box(
                    modifier = Modifier
                        .weight((chapter.endMs - chapter.startMs).coerceAtLeast(1L).toFloat())
                        .fillMaxHeight()
                        .background(
                            if (selected) {
                                Color.White.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.05f)
                            }
                        )
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = if (selected) 0.28f else 0.12f)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chapter.title,
                        color = Color.White.copy(alpha = if (selected) 0.98f else 0.76f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
