package dev.aaa1115910.bv.screen.live

internal enum class LiveOverlayPanel {
    None,
    RightMenu,
    BottomMenu,
    UpSpace
}

internal fun openLiveRightMenu(): LiveOverlayPanel = LiveOverlayPanel.RightMenu

internal fun openLiveBottomMenu(): LiveOverlayPanel = LiveOverlayPanel.BottomMenu

internal fun toggleLiveRightMenu(activeOverlay: LiveOverlayPanel): LiveOverlayPanel {
    return if (activeOverlay == LiveOverlayPanel.RightMenu) {
        LiveOverlayPanel.None
    } else {
        LiveOverlayPanel.RightMenu
    }
}

internal data class LiveBackPressResult(
    val activeOverlay: LiveOverlayPanel,
    val lastBackPressedAt: Long,
    val statusText: String?,
    val shouldExit: Boolean
)

internal fun handleLiveBackPress(
    activeOverlay: LiveOverlayPanel,
    lastBackPressedAt: Long,
    now: Long,
    exitIntervalMs: Long = 2_000L
): LiveBackPressResult {
    if (activeOverlay != LiveOverlayPanel.None) {
        return LiveBackPressResult(
            activeOverlay = LiveOverlayPanel.None,
            lastBackPressedAt = lastBackPressedAt,
            statusText = null,
            shouldExit = false
        )
    }
    val shouldExit = lastBackPressedAt > 0L && now - lastBackPressedAt < exitIntervalMs
    return if (shouldExit) {
        LiveBackPressResult(
            activeOverlay = LiveOverlayPanel.None,
            lastBackPressedAt = lastBackPressedAt,
            statusText = null,
            shouldExit = true
        )
    } else {
        LiveBackPressResult(
            activeOverlay = LiveOverlayPanel.None,
            lastBackPressedAt = now,
            statusText = "再次按下返回键退出播放",
            shouldExit = false
        )
    }
}
