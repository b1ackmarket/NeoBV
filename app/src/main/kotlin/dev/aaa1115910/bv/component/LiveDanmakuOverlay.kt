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
    private var nextRollingLane = 0
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
        nextRollingLane = 0
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
        val lane = when (event.displayMode) {
            DisplayMode.Top -> 0
            DisplayMode.Bottom -> maxLaneCount - 1
            DisplayMode.Rolling -> nextRollingLane.also {
                nextRollingLane = (nextRollingLane + 1) % maxLaneCount
            }
        }
        val durationMs = when (event.displayMode) {
            DisplayMode.Rolling -> (8_000L / state.speedFactor.coerceIn(0.4f, 3f)).toLong()
            DisplayMode.Top,
            DisplayMode.Bottom -> 4_000L
        }.coerceAtLeast(2_500L)
        val alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
        return RenderItem(
            id = id,
            text = event.content,
            color = (alpha shl 24) or (event.color and 0x00ffffff),
            startAtMs = nowMs + delayMs,
            durationMs = durationMs,
            lane = lane,
            laneHeight = laneHeight,
            textWidth = textWidth,
            textSizePx = textSizePx,
            displayMode = event.displayMode
        )
    }

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
    }
}

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
