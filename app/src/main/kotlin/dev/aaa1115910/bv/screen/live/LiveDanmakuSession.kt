package dev.aaa1115910.bv.screen.live

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.bv.component.LiveDanmakuOverlayController
import dev.aaa1115910.bv.component.LiveDanmakuOverlayItem

class LiveDanmakuSession {
    val overlayController = LiveDanmakuOverlayController()
    private var nextDanmakuId = 1L

    fun start() {
        overlayController.setPlaying(true)
    }

    fun pause() {
        overlayController.setPlaying(false)
    }

    fun addDanmaku(event: DanmakuEvent, isPlaying: Boolean) {
        if (!overlayController.allows(event)) return
        val item = LiveDanmakuOverlayItem(
            id = nextDanmakuId++,
            event = event,
            positionMs = 0L
        )
        overlayController.append(
            item = item,
            delayMs = 0L
        )
        if (isPlaying) {
            start()
        }
    }

    fun clear() {
        nextDanmakuId = 1L
        overlayController.clear()
        overlayController.setPlaying(false)
    }

    fun release() {
        overlayController.release()
    }
}
