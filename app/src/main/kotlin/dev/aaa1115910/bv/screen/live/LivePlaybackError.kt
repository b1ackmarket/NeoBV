package dev.aaa1115910.bv.screen.live

import dev.aaa1115910.bv.repository.LivePlaybackSource

internal fun resolveLivePlaybackErrorMessage(
    liveStatus: Int?,
    source: LivePlaybackSource?,
    throwable: Throwable? = null
): String? {
    if (throwable != null) {
        return throwable.message?.takeIf { it.isNotBlank() } ?: "直播加载失败，请稍后重试"
    }
    if (liveStatus != null && liveStatus != 1) {
        return "主播暂未开播"
    }
    if (source == null) {
        return "无法获取直播流地址，请稍后重试"
    }
    return null
}
