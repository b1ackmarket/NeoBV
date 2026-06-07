package dev.aaa1115910.bv.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.LiveDanmakuMenuState
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun LiveDanmakuOverlay(
    modifier: Modifier = Modifier,
    controller: LiveDanmakuOverlayController,
    state: LiveDanmakuMenuState
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LiveDanmakuView(context).also { controller.bind(it) }
        },
        update = { view ->
            controller.bind(view)
            controller.applyState(state)
        },
        onRelease = { view ->
            controller.unbind(view)
        }
    )
}

class LiveDanmakuOverlayController {
    private var view: LiveDanmakuView? = null
    private var state: LiveDanmakuMenuState? = null
    private var playing: Boolean = false

    fun bind(view: LiveDanmakuView) {
        this.view = view
        state?.let(view::applyState)
        view.setPlaying(playing)
    }

    fun unbind(view: LiveDanmakuView) {
        if (this.view === view) {
            this.view = null
        }
    }

    fun applyState(state: LiveDanmakuMenuState) {
        this.state = state
        view?.applyState(state)
    }

    fun setPlaying(playing: Boolean) {
        this.playing = playing
        view?.setPlaying(playing)
    }

    fun append(item: LiveDanmakuOverlayItem, delayMs: Long = 0L) {
        view?.append(item, delayMs)
    }

    fun append(items: List<LiveDanmakuOverlayItem>, currentPositionMs: Long) {
        if (items.isEmpty()) return
        view?.append(
            items = items,
            currentPositionMs = currentPositionMs
        )
    }

    fun clear() {
        view?.clear()
    }

    fun allows(event: DanmakuEvent): Boolean = state?.allows(event) == true

    fun release() {
        view?.clear()
        view = null
    }
}

data class LiveDanmakuOverlayItem(
    val id: Long,
    val event: DanmakuEvent,
    val positionMs: Long
)

class LiveDanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var state: LiveDanmakuMenuState? = null
    private var playing = false
    private var pauseStartedAtMs = 0L
    private val items = mutableListOf<RenderItem>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.BLACK
    }

    fun applyState(state: LiveDanmakuMenuState) {
        this.state = state
        if (state.enabledTypes.isEmpty()) {
            clear()
        } else {
            invalidate()
        }
    }

    fun setPlaying(playing: Boolean) {
        if (this.playing == playing) return
        val now = SystemClock.uptimeMillis()
        if (playing) {
            if (pauseStartedAtMs > 0L) {
                val pausedDuration = now - pauseStartedAtMs
                for (index in items.indices) {
                    items[index] = items[index].copy(startAtMs = items[index].startAtMs + pausedDuration)
                }
            }
            pauseStartedAtMs = 0L
            postInvalidateOnAnimation()
        } else {
            pauseStartedAtMs = now
        }
        this.playing = playing
    }

    fun append(item: LiveDanmakuOverlayItem, delayMs: Long = 0L) {
        val config = state ?: return
        if (!config.allows(item.event)) return
        if (width <= 0 || height <= 0 || item.event.content.isBlank()) return
        items += item.toRenderItem(
            nowMs = SystemClock.uptimeMillis(),
            delayMs = delayMs.coerceAtLeast(0L),
            state = config
        )
        trim(nowMs = SystemClock.uptimeMillis())
        postInvalidateOnAnimation()
    }

    fun append(items: List<LiveDanmakuOverlayItem>, currentPositionMs: Long) {
        if (items.isEmpty()) return
        items.forEach { item ->
            append(
                item = item,
                delayMs = item.positionMs - currentPositionMs
            )
        }
    }

    fun clear() {
        items.clear()
        pauseStartedAtMs = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val config = state ?: return
        if (config.enabledTypes.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.isExpired(now)) {
                iterator.remove()
                continue
            }
            if (now < item.startAtMs) continue
            drawItem(canvas, item, now)
        }
        if (playing && items.isNotEmpty()) {
            postInvalidateOnAnimation()
        }
    }

    private fun LiveDanmakuOverlayItem.toRenderItem(
        nowMs: Long,
        delayMs: Long,
        state: LiveDanmakuMenuState
    ): RenderItem {
        val textSizePx = sp(18f * state.scale.coerceIn(0.5f, 4f))
        textPaint.textSize = textSizePx
        strokePaint.textSize = textSizePx
        val textWidth = textPaint.measureText(event.content)
        val fontMetrics = textPaint.fontMetrics
        val laneHeight = (fontMetrics.descent - fontMetrics.ascent + dp(8f)).coerceAtLeast(dp(22f))
        val maxLaneCount = max(1, ((height * state.area.coerceIn(0.05f, 1f)) / laneHeight).toInt())
        val requestedStartAtMs = nowMs + delayMs
        val durationMs = when (event.displayMode) {
            DisplayMode.Rolling -> (8_000L / state.speedFactor.coerceIn(0.4f, 3f)).toLong()
            DisplayMode.Top,
            DisplayMode.Bottom -> FixedDanmakuDurationMs
        }.coerceAtLeast(2_500L)
        val placement = when (event.displayMode) {
            DisplayMode.Top -> RollingDanmakuLayouter.Placement(
                lane = chooseFixedLane(
                    displayMode = DisplayMode.Top,
                    laneCount = maxLaneCount,
                    startAtMs = requestedStartAtMs,
                    durationMs = durationMs,
                    preferredLane = preferredLane(maxLaneCount)
                ),
                startAtMs = requestedStartAtMs
            )

            DisplayMode.Bottom -> RollingDanmakuLayouter.Placement(
                lane = chooseFixedLane(
                    displayMode = DisplayMode.Bottom,
                    laneCount = maxLaneCount,
                    startAtMs = requestedStartAtMs,
                    durationMs = durationMs,
                    preferredLane = maxLaneCount - 1 - preferredLane(maxLaneCount)
                ),
                startAtMs = requestedStartAtMs
            )

            DisplayMode.Rolling -> chooseRollingPlacement(
                laneCount = maxLaneCount,
                requestedStartAtMs = requestedStartAtMs,
                textWidth = textWidth,
                durationMs = durationMs,
                preferredLane = preferredLane(maxLaneCount)
            )
        }
        val alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
        return RenderItem(
            id = id,
            text = event.content,
            color = (alpha shl 24) or (event.color and 0x00ffffff),
            startAtMs = placement.startAtMs,
            durationMs = durationMs,
            lane = placement.lane,
            laneHeight = laneHeight,
            textWidth = textWidth,
            textSizePx = textSizePx,
            displayMode = event.displayMode
        )
    }

    private fun LiveDanmakuOverlayItem.preferredLane(laneCount: Int): Int {
        if (laneCount <= 1) return 0
        val seed = (id * 31 + event.content.hashCode()).toInt()
        return seed.floorMod(laneCount)
    }

    private fun chooseRollingPlacement(
        laneCount: Int,
        requestedStartAtMs: Long,
        textWidth: Float,
        durationMs: Long,
        preferredLane: Int
    ): RollingDanmakuLayouter.Placement {
        val minGap = max(dp(18f), textWidth * 0.08f)
        return RollingDanmakuLayouter.place(
            laneCount = laneCount,
            requestedStartAtMs = requestedStartAtMs,
            displayWidthPx = width.toFloat(),
            newWidthPx = textWidth,
            newDurationMs = durationMs,
            minGapPx = minGap,
            preferredLane = preferredLane,
            activeItems = items
                .asSequence()
                .filter { item ->
                    item.displayMode == DisplayMode.Rolling &&
                        item.lane in 0 until laneCount &&
                        item.startAtMs + item.durationMs > requestedStartAtMs
                }
                .map { item ->
                    RollingDanmakuLayouter.Item(
                        lane = item.lane,
                        startAtMs = item.startAtMs,
                        durationMs = item.durationMs,
                        widthPx = item.textWidth
                    )
                }
                .toList()
        )
    }

    private fun chooseFixedLane(
        displayMode: DisplayMode,
        laneCount: Int,
        startAtMs: Long,
        durationMs: Long,
        preferredLane: Int
    ): Int {
        val candidates = when (displayMode) {
            DisplayMode.Bottom -> orderedLaneCandidates(laneCount, preferredLane).map { laneCount - 1 - it }
            else -> orderedLaneCandidates(laneCount, preferredLane)
        }
        return candidates.firstOrNull { lane ->
            items.none { item ->
                item.displayMode == displayMode &&
                    item.lane == lane &&
                    item.startAtMs < startAtMs + durationMs &&
                    item.startAtMs + item.durationMs > startAtMs
            }
        } ?: candidates.minBy { lane ->
            items
                .filter { item -> item.displayMode == displayMode && item.lane == lane && !item.isExpired(startAtMs) }
                .minOfOrNull { item -> item.startAtMs + item.durationMs }
                ?: startAtMs
        }
    }

    private fun orderedLaneCandidates(laneCount: Int, preferredLane: Int): List<Int> =
        List(laneCount) { offset -> (preferredLane + offset).floorMod(laneCount) }

    private fun drawItem(canvas: Canvas, item: RenderItem, nowMs: Long) {
        textPaint.textSize = item.textSizePx
        strokePaint.textSize = item.textSizePx
        textPaint.color = item.color
        strokePaint.color = Color.argb(Color.alpha(item.color), 0, 0, 0)
        val progress = ((nowMs - item.startAtMs).toFloat() / item.durationMs).coerceIn(0f, 1f)
        val x = when (item.displayMode) {
            DisplayMode.Rolling -> width - progress * (width + item.textWidth)
            DisplayMode.Top,
            DisplayMode.Bottom -> (width - item.textWidth) / 2f
        }
        val unclampedY = when (item.displayMode) {
            DisplayMode.Bottom -> height - (item.lane + 1) * item.laneHeight + item.textSizePx
            else -> item.lane * item.laneHeight + item.textSizePx
        }
        val maxY = (height.toFloat() - dp(8f)).coerceAtLeast(item.textSizePx)
        val y = unclampedY.coerceIn(item.textSizePx, maxY)
        canvas.drawText(item.text, x, y, strokePaint)
        canvas.drawText(item.text, x, y, textPaint)
    }

    private fun trim(nowMs: Long) {
        items.removeAll { it.isExpired(nowMs) }
        while (items.size > MaxItems) {
            items.removeAt(0)
        }
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private data class RenderItem(
        val id: Long,
        val text: String,
        val color: Int,
        val startAtMs: Long,
        val durationMs: Long,
        val lane: Int,
        val laneHeight: Float,
        val textWidth: Float,
        val textSizePx: Float,
        val displayMode: DisplayMode
    ) {
        fun isExpired(nowMs: Long): Boolean = nowMs - startAtMs > durationMs
    }

    private companion object {
        const val MaxItems = 400
        const val FixedDanmakuDurationMs = 4_000L
    }
}

internal object RollingDanmakuLayouter {
    data class Item(
        val lane: Int,
        val startAtMs: Long,
        val durationMs: Long,
        val widthPx: Float
    )

    data class Placement(
        val lane: Int,
        val startAtMs: Long
    )

    fun place(
        laneCount: Int,
        requestedStartAtMs: Long,
        displayWidthPx: Float,
        newWidthPx: Float,
        newDurationMs: Long,
        minGapPx: Float,
        preferredLane: Int,
        activeItems: List<Item>
    ): Placement {
        val normalizedLaneCount = laneCount.coerceAtLeast(1)
        val normalizedPreferredLane = preferredLane.floorMod(normalizedLaneCount)
        val candidates = (0 until normalizedLaneCount).map { lane ->
            val startAtMs = earliestSafeStart(
                lane = lane,
                requestedStartAtMs = requestedStartAtMs,
                displayWidthPx = displayWidthPx.coerceAtLeast(1f),
                newWidthPx = newWidthPx.coerceAtLeast(0f),
                newDurationMs = newDurationMs.coerceAtLeast(1L),
                minGapPx = minGapPx.coerceAtLeast(0f),
                activeItems = activeItems
            )
            Candidate(
                lane = lane,
                startAtMs = startAtMs,
                delayMs = startAtMs - requestedStartAtMs,
                preferredDistance = circularDistance(lane, normalizedPreferredLane, normalizedLaneCount)
            )
        }
        val best = candidates.minWithOrNull(
            compareBy<Candidate> { it.delayMs }
                .thenBy { it.preferredDistance }
                .thenBy { it.lane }
        ) ?: Candidate(
            lane = normalizedPreferredLane,
            startAtMs = requestedStartAtMs,
            delayMs = 0L,
            preferredDistance = 0
        )
        return Placement(lane = best.lane, startAtMs = best.startAtMs)
    }

    private fun earliestSafeStart(
        lane: Int,
        requestedStartAtMs: Long,
        displayWidthPx: Float,
        newWidthPx: Float,
        newDurationMs: Long,
        minGapPx: Float,
        activeItems: List<Item>
    ): Long {
        val laneItems = activeItems
            .filter { item -> item.lane == lane && item.durationMs > 0L && item.widthPx >= 0f }
            .sortedBy { item -> item.startAtMs }
        var candidateStartAtMs = requestedStartAtMs
        repeat(MaxPlacementIterations) {
            var nextStartAtMs = candidateStartAtMs
            laneItems.forEach { item ->
                val delayMs = delayRequiredForItem(
                    item = item,
                    candidateStartAtMs = candidateStartAtMs,
                    displayWidthPx = displayWidthPx,
                    newWidthPx = newWidthPx,
                    newDurationMs = newDurationMs,
                    minGapPx = minGapPx
                )
                if (delayMs > 0L) {
                    nextStartAtMs = max(nextStartAtMs, candidateStartAtMs + delayMs)
                }
            }
            if (nextStartAtMs == candidateStartAtMs) return candidateStartAtMs
            candidateStartAtMs = nextStartAtMs
        }
        return candidateStartAtMs
    }

    private fun delayRequiredForItem(
        item: Item,
        candidateStartAtMs: Long,
        displayWidthPx: Float,
        newWidthPx: Float,
        newDurationMs: Long,
        minGapPx: Float
    ): Long {
        if (item.startAtMs <= candidateStartAtMs) {
            return delayRequiredBehindFront(
                front = item,
                behindStartAtMs = candidateStartAtMs,
                behindDurationMs = newDurationMs,
                behindWidthPx = newWidthPx,
                displayWidthPx = displayWidthPx,
                minGapPx = minGapPx
            )
        }
        if (item.startAtMs >= candidateStartAtMs + newDurationMs) return 0L
        if (!futureItemWouldCollide(
                futureItem = item,
                candidateStartAtMs = candidateStartAtMs,
                displayWidthPx = displayWidthPx,
                newWidthPx = newWidthPx,
                newDurationMs = newDurationMs,
                minGapPx = minGapPx
            )
        ) {
            return 0L
        }
        val safeStartAfterFutureItem = item.startAtMs + delayRequiredBehindFront(
            front = item,
            behindStartAtMs = item.startAtMs,
            behindDurationMs = newDurationMs,
            behindWidthPx = newWidthPx,
            displayWidthPx = displayWidthPx,
            minGapPx = minGapPx
        )
        return (safeStartAfterFutureItem - candidateStartAtMs).coerceAtLeast(0L)
    }

    private fun futureItemWouldCollide(
        futureItem: Item,
        candidateStartAtMs: Long,
        displayWidthPx: Float,
        newWidthPx: Float,
        newDurationMs: Long,
        minGapPx: Float
    ): Boolean {
        val candidateAgeMs = futureItem.startAtMs - candidateStartAtMs
        if (candidateAgeMs >= newDurationMs) return false
        val frontSpeedPxMs = pixelsPerMs(displayWidthPx, newWidthPx, newDurationMs)
        val behindSpeedPxMs = pixelsPerMs(displayWidthPx, futureItem.widthPx, futureItem.durationMs)
        val frontLeftPx = displayWidthPx - frontSpeedPxMs * candidateAgeMs
        val frontRightPx = frontLeftPx + newWidthPx
        val initialGapPx = displayWidthPx - frontRightPx
        val remainingFrontMs = newDurationMs - candidateAgeMs
        val catchUpGapPx = if (behindSpeedPxMs > frontSpeedPxMs) {
            (behindSpeedPxMs - frontSpeedPxMs) * remainingFrontMs
        } else {
            0f
        }
        return minGapPx + catchUpGapPx > initialGapPx
    }

    private fun delayRequiredBehindFront(
        front: Item,
        behindStartAtMs: Long,
        behindDurationMs: Long,
        behindWidthPx: Float,
        displayWidthPx: Float,
        minGapPx: Float
    ): Long {
        val frontAgeMs = behindStartAtMs - front.startAtMs
        if (frontAgeMs >= front.durationMs) return 0L
        if (frontAgeMs < 0L) return front.startAtMs - behindStartAtMs
        val frontSpeedPxMs = pixelsPerMs(displayWidthPx, front.widthPx, front.durationMs)
        val behindSpeedPxMs = pixelsPerMs(displayWidthPx, behindWidthPx, behindDurationMs)
        val frontLeftPx = displayWidthPx - frontSpeedPxMs * frontAgeMs
        val frontRightPx = frontLeftPx + front.widthPx
        val initialGapPx = displayWidthPx - frontRightPx
        val remainingFrontMs = front.durationMs - frontAgeMs
        val catchUpGapPx = if (behindSpeedPxMs > frontSpeedPxMs) {
            (behindSpeedPxMs - frontSpeedPxMs) * remainingFrontMs
        } else {
            0f
        }
        val missingGapPx = minGapPx + catchUpGapPx - initialGapPx
        if (missingGapPx <= 0f) return 0L
        val delayMs = ceil(missingGapPx / behindSpeedPxMs.coerceAtLeast(0.001f)).toLong()
        return delayMs.coerceIn(0L, remainingFrontMs)
    }

    private fun pixelsPerMs(displayWidthPx: Float, itemWidthPx: Float, durationMs: Long): Float =
        (displayWidthPx + itemWidthPx).coerceAtLeast(1f) / durationMs.coerceAtLeast(1L)

    private fun circularDistance(lane: Int, preferredLane: Int, laneCount: Int): Int {
        val forward = (lane - preferredLane).floorMod(laneCount)
        val backward = (preferredLane - lane).floorMod(laneCount)
        return minOf(forward, backward)
    }

    private data class Candidate(
        val lane: Int,
        val startAtMs: Long,
        val delayMs: Long,
        val preferredDistance: Int
    )

    private const val MaxPlacementIterations = 8
}

private fun Int.floorMod(mod: Int): Int =
    ((this % mod) + mod) % mod

private fun LiveDanmakuMenuState.allows(event: DanmakuEvent): Boolean {
    if (enabledTypes.isEmpty()) return false
    return when (event.displayMode) {
        DisplayMode.Rolling -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Rolling)
        DisplayMode.Top -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Top)
        DisplayMode.Bottom -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Bottom)
    }
}

private val DanmakuEvent.displayMode: DisplayMode
    get() = when (mode) {
        4 -> DisplayMode.Bottom
        5 -> DisplayMode.Top
        else -> DisplayMode.Rolling
    }

private enum class DisplayMode {
    Rolling,
    Top,
    Bottom
}
