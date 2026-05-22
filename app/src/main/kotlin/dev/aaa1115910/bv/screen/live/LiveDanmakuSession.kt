package dev.aaa1115910.bv.screen.live

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.bv.component.LiveDanmakuOverlayController
import dev.aaa1115910.bv.component.LiveDanmakuOverlayItem

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
    val overlayController = LiveDanmakuOverlayController()
    private val timeline = LiveDanmakuTimeline()
    private val appendQueue = LiveDanmakuAppendQueue(currentPositionMs = { currentPositionMs() })
    private val seenDanmakuKeys = linkedSetOf<String>()
    private val seenLooseDanmakuKeys = linkedSetOf<String>()

    fun start() {
        timeline.start()
        overlayController.setPlaying(true)
    }

    fun pause() {
        timeline.pause()
        overlayController.setPlaying(false)
    }

    fun currentPositionMs(): Long = timeline.currentPositionMs()

    fun addDanmaku(event: DanmakuEvent, isPlaying: Boolean) {
        if (!markSeen(event)) return
        addSeenDanmaku(event, isPlaying)
    }

    fun addSeenDanmaku(event: DanmakuEvent, isPlaying: Boolean) {
        if (!overlayController.allows(event)) return
        val item = appendQueue.add(event)
        overlayController.append(
            item = item,
            delayMs = item.positionMs - currentPositionMs()
        )
        if (isPlaying) {
            start()
        }
    }

    fun appendDanmaku(events: List<DanmakuEvent>, isPlaying: Boolean) {
        if (events.isEmpty()) return
        val newEvents = events.filter(::markSeen)
        if (newEvents.isEmpty()) return
        appendQueue.seed(newEvents)
        overlayController.append(
            items = appendQueue.snapshot(),
            currentPositionMs = currentPositionMs()
        )
        if (isPlaying) {
            start()
        }
    }

    fun seedRecentDanmaku(events: List<DanmakuEvent>, isPlaying: Boolean) {
        if (events.isEmpty()) return
        val newEvents = events.takeLast(25).filter(::markSeen)
        if (newEvents.isEmpty()) return
        if (isPlaying) {
            start()
        }
    }

    fun markSeen(events: List<DanmakuEvent>) {
        events.forEach(::markSeenInternal)
    }

    fun markSeen(event: DanmakuEvent): Boolean = markSeenInternal(event)

    fun clear() {
        appendQueue.clear()
        seenDanmakuKeys.clear()
        seenLooseDanmakuKeys.clear()
        timeline.clear()
        overlayController.clear()
        overlayController.setPlaying(false)
    }

    fun release() {
        overlayController.release()
    }

    fun hasSeen(event: DanmakuEvent): Boolean =
        event.stableKey() in seenDanmakuKeys || event.looseStableKey() in seenLooseDanmakuKeys

    internal fun markSeenInternal(event: DanmakuEvent): Boolean {
        if (!seenLooseDanmakuKeys.add(event.looseStableKey())) return false
        seenDanmakuKeys.add(event.stableKey())
        while (seenLooseDanmakuKeys.size > 3000) seenLooseDanmakuKeys.remove(seenLooseDanmakuKeys.first())
        while (seenDanmakuKeys.size > 3000) seenDanmakuKeys.remove(seenDanmakuKeys.first())
        return true
    }
}

internal fun DanmakuEvent.stableKey(): String {
    val time = rndTimeMs ?: sendTimeMs ?: eventTimeMs
    return "$time|$mid|$username|$content"
}

internal fun DanmakuEvent.looseStableKey(): String = "${mid.takeIf { it > 0 } ?: username}|$content"

internal class LiveDanmakuAppendQueue(
    private val currentPositionMs: () -> Long,
    private val leadTimeMs: Long = 500L,
    private val minGapMs: Long = 120L,
    private val maxItems: Int = 2000
) {
    private var lastAppendPositionMs = 0L
    private val items = mutableListOf<LiveDanmakuOverlayItem>()
    private var nextDanmakuId = 1L

    fun add(event: DanmakuEvent): LiveDanmakuOverlayItem {
        val item = buildItem(
            event = event,
            positionMs = nextPositionMs()
        )
        items += item
        trim()
        return item
    }

    fun seed(events: List<DanmakuEvent>): List<LiveDanmakuOverlayItem> {
        if (events.isEmpty()) return emptyList()
        val currentPositionMs = currentPositionMs()
        val seedStart = (currentPositionMs - (events.size * 700L)).coerceAtLeast(0L)
        return events.mapIndexed { index, event ->
            val positionMs = seedStart + index * 700L
            buildItem(event = event, positionMs = positionMs).also { item ->
                items += item
            }
        }.also { trim() }
    }

    fun snapshot(): List<LiveDanmakuOverlayItem> = items.toList()

    fun clear() {
        lastAppendPositionMs = 0L
        nextDanmakuId = 1L
        items.clear()
    }

    private fun buildItem(event: DanmakuEvent, positionMs: Long): LiveDanmakuOverlayItem {
        return LiveDanmakuOverlayItem(
            id = nextDanmakuId++,
            positionMs = positionMs,
            event = event
        )
    }

    private fun nextPositionMs(): Long {
        val next = (currentPositionMs() + leadTimeMs).coerceAtLeast(lastAppendPositionMs + minGapMs)
        lastAppendPositionMs = next
        return next
    }

    private fun trim() {
        while (items.size > maxItems) items.removeAt(0)
    }
}
