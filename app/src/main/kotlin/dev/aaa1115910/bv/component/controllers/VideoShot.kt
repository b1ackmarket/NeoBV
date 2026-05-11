package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bv.util.SpriteFrame
import dev.aaa1115910.bv.util.VideoShotImageCache
import dev.aaa1115910.bv.util.getSpriteFrame
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VideoShot(
    modifier: Modifier = Modifier,
    videoShot: VideoShot,
    imageCache: VideoShotImageCache,
    position: Long,
    duration: Long,
    coercedOffset: Dp = 0.dp,
    centerPreview: Boolean = false,
    previewHeight: Dp = 100.dp
) {
    val view = LocalView.current
    var spriteFrame by remember { mutableStateOf<SpriteFrame?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        LaunchedEffect(position) {
            if (view.isInEditMode) return@LaunchedEffect
            delay(16)
            spriteFrame = videoShot.getSpriteFrame(position.toInt() / 1000, imageCache)
        }

        spriteFrame?.let { frame ->
            VideoShotImage(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        if (centerPreview) {
                            return@layout layout(placeable.width, placeable.height) {
                                placeable.placeRelative(
                                    x = ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0),
                                    y = 0
                                )
                            }
                        }

                        val containerWidthPx = constraints.maxWidth
                        val imageWidthPx = placeable.width
                        val coercedOffsetPx = coercedOffset.roundToPx()

                        val xPosition = if (duration <= 0L) {
                            0
                        } else {
                            val progress = position.toDouble() / duration.toDouble()
                            val rawOffset = (-imageWidthPx / 2.0) + (containerWidthPx * progress)

                            val minOffset = coercedOffsetPx.toDouble()
                            val maxOffset =
                                (containerWidthPx - imageWidthPx - coercedOffsetPx).toDouble()

                            rawOffset.coerceIn(minOffset, maxOffset).toInt()
                        }

                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(x = xPosition, y = 0)
                        }
                    },
                spriteFrame = frame,
                previewHeight = previewHeight
            )
        }
    }
}

@Composable
fun VideoShotImage(
    modifier: Modifier = Modifier,
    spriteFrame: SpriteFrame,
    previewHeight: Dp = 100.dp
) {
    val view = LocalView.current

    // 计算纵横比：确保预览框比例正确
    val aspectRatio = spriteFrame.srcRect.width.toFloat() / spriteFrame.srcRect.height

    Spacer(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .height(previewHeight)
            .aspectRatio(aspectRatio)
            .shadow(10.dp, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        alpha = 0.4f
                    )

                    // 直接绘制大图的局部区域到画布，零像素拷贝
                    drawImage(
                        image = spriteFrame.spriteSheet,
                        srcOffset = spriteFrame.srcRect.topLeft,
                        srcSize = spriteFrame.srcRect.size,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.Low
                    )

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.08f)
                    )

                    if (view.isInEditMode) {
                        drawLine(
                            Color.White,
                            Offset(center.x, 0f),
                            Offset(center.y, size.height),
                            2f
                        )
                    }
                }
            }
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun VideoShotPreview(@PreviewParameter(VideoShotProgressProvider::class) data: Pair<Long, Long>) {
    MaterialTheme {
        Column {
            VideoShot(
                videoShot = VideoShot(
                    times = emptyList(),
                    imageCountX = 0,
                    imageCountY = 0,
                    imageWidth = 0,
                    imageHeight = 0,
                    images = emptyList()
                ),
                imageCache = VideoShotImageCache(),
                position = data.second,
                duration = data.first
            )
            VideoProgressSeek(
                duration = data.first,
                position = data.second,
                bufferedPercentage = 1,
                isPersistentSeek = false
            )
        }
    }
}

private class VideoShotProgressProvider : PreviewParameterProvider<Pair<Long, Long>> {
    override val values = sequenceOf(
        Pair(1234_000L, 0L),
        Pair(1234_000L, 234_000L),
        Pair(1234_000L, 555_000L),
        Pair(1234_000L, 1234_000L)
    )
}
