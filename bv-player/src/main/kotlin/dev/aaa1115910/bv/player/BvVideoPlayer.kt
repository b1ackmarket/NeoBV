package dev.aaa1115910.bv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer

@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer?,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FILL,
    keepScreenAwake: Boolean = false,
) {
    if (videoPlayer is ExoMediaPlayer) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = videoPlayer.mPlayer
                    this.resizeMode = resizeMode
                    useController = false
                    if (keepScreenAwake) {
                        keepScreenOn = true
                    }
                }
            },
            update = { playerView ->
                playerView.player = videoPlayer.mPlayer
                playerView.resizeMode = resizeMode
                if (keepScreenAwake) {
                    playerView.keepScreenOn = true
                }
            },
            onRelease = { playerView ->
                playerView.player = null
                playerView.keepScreenOn = false
            }
        )
    }
}
