package dev.aaa1115910.bv.screen.live

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.LiveDanmakuMenuState

internal class LiveDanmakuTimeline(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private var anchorMs: Long? = null
    private var pausedPositionMs: Long = 0L

    fun start() {
        if (anchorMs == null) {
            anchorMs = nowMs() - pausedPositionMs
        }
    }

    fun pause() {
        pausedPositionMs = currentPositionMs()
        anchorMs = null
    }

    fun clear() {
        anchorMs = null
        pausedPositionMs = 0L
    }

    fun currentPositionMs(): Long {
        val anchor = anchorMs ?: return pausedPositionMs
        return (nowMs() - anchor).coerceAtLeast(0L)
    }
}

class LiveDanmakuSession {
    val player = DanmakuPlayer(SimpleRenderer())

    private var danmakuConfig = DanmakuConfig()
    private val danmakuTypeFilter = TypeFilter()
    private val items = mutableListOf<DanmakuItemData>()
    private val timeline = LiveDanmakuTimeline()
    private var nextDanmakuId = 1L

    fun applyState(state: LiveDanmakuMenuState) {
        danmakuTypeFilter.clear()
        if (!state.enabledTypes.contains(DanmakuType.All)) {
            val disabledTypes = DanmakuType.entries.toMutableList().apply {
                remove(DanmakuType.All)
                removeAll(state.enabledTypes)
            }
            disabledTypes.mapNotNull { type ->
                when (type) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }.forEach { danmakuTypeFilter.addFilterItem(it) }
        }

        danmakuConfig = danmakuConfig.copy(
            density = 120,
            textSizeScale = state.scale,
            screenPart = state.area,
            dataFilter = listOf(danmakuTypeFilter),
            rollingSpeedFactor = state.speedFactor
        )
        danmakuConfig.updateFilter()
        player.updateConfig(danmakuConfig)
        player.setDanmakuRollingSpeed(state.speedFactor)
    }

    fun start() {
        timeline.start()
        player.start()
    }

    fun pause() {
        timeline.pause()
        player.pause()
    }

    fun currentPositionMs(): Long = timeline.currentPositionMs()

    fun addDanmaku(event: DanmakuEvent, isPlaying: Boolean) {
        val positionMs = currentPositionMs() + 500L
        items += DanmakuItemData(
            danmakuId = nextDanmakuId++,
            position = positionMs,
            content = event.content,
            mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
            textSize = 25,
            textColor = Color.White.toArgb()
        )
        if (items.size > 500) {
            items.removeAt(0)
        }
        player.updateData(items.toList())
        player.seekTo(positionMs)
        if (isPlaying) {
            start()
        }
    }

    fun seedRecentDanmaku(events: List<DanmakuEvent>, isPlaying: Boolean) {
        if (events.isEmpty()) return
        val currentPositionMs = currentPositionMs()
        val recentEvents = events.takeLast(25)
        val seedStart = (currentPositionMs - (recentEvents.size * 700L)).coerceAtLeast(0L)
        recentEvents.forEachIndexed { index, event ->
            items += DanmakuItemData(
                danmakuId = nextDanmakuId++,
                position = seedStart + index * 700L,
                content = event.content,
                mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
                textSize = 25,
                textColor = Color.White.toArgb()
            )
        }
        player.updateData(items.toList())
        player.seekTo(currentPositionMs)
        if (isPlaying) {
            start()
        }
    }

    fun clear() {
        items.clear()
        nextDanmakuId = 1L
        timeline.clear()
        player.updateData(emptyList())
        player.seekTo(0)
        player.pause()
    }

    fun release() {
        player.release()
    }
}
