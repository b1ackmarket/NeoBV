package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun PlayStateTips(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isError: Boolean,
    errorMessage: String? = null,
    tcpSpeedBps: Long = 0L
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (!isPlaying && !isBuffering && !isError) {
            PauseIcon(
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
        if (isBuffering && !isError) {
            BufferingTip(
                modifier = Modifier
                    .align(Alignment.Center),
                speed = tcpSpeedBps.toNetSpeedText()
            )
        }
        if (isError) {
            PlayErrorTip(
                modifier = Modifier.align(Alignment.Center),
                errorMessage = errorMessage
            )
        }
    }
}

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(96.dp),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.88f)
        )
    }
}

@Composable
fun BufferingTip(
    modifier: Modifier = Modifier,
    speed: String
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = Color.Black.copy(0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .padding(8.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Text(
                modifier = Modifier,
                text = "缓冲中...$speed",
                fontSize = 24.sp
            )
        }
    }
}

/**
 * 将 bits/second 转换为人类可读的网速字符串
 * >= 1 MB/s 用 MB 单位，< 1 MB/s 用 KB 单位
 */
private fun Long.toNetSpeedText(): String {
    if (this <= 0L) return ""
    val kbps = this / 8.0 / 1024.0
    return if (kbps >= 1024) {
        " · ${String.format("%.1f", kbps / 1024)} MB/s"
    } else {
        " · ${String.format("%.1f", kbps)} KB/s"
    }
}

@Composable
fun PlayErrorTip(
    modifier: Modifier = Modifier,
    errorMessage: String?
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = Color.Black.copy(0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp, 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "播放器正在抽风",
                style = MaterialTheme.typography.titleLarge
            )
            Text(text = " _(:з」∠)_")
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "错误信息：${errorMessage ?: "未知错误"}")
        }
    }
}

@Preview
@Composable
private fun PauseIconPreview() {
    BVTheme {
        Box(modifier = Modifier.padding(10.dp)) {
            PauseIcon()
        }
    }
}

@Preview
@Composable
private fun BufferingTipPreview() {
    BVTheme {
        BufferingTip(
            modifier = Modifier.padding(10.dp),
            speed = ""
        )
    }
}

@Preview
@Composable
private fun PlayErrorTipPreview() {
    BVTheme {
        PlayErrorTip(errorMessage = "This is a test error.")
    }
}
