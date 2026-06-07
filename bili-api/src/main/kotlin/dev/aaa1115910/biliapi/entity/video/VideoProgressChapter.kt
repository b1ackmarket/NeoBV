package dev.aaa1115910.biliapi.entity.video

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class VideoProgressChapter(
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val cover: String = "",
    val type: Int = 0
) {
    companion object {
        fun fromViewPoints(
            viewPoints: JsonArray?,
            durationMs: Long
        ): List<VideoProgressChapter> {
            if (viewPoints == null) return emptyList()
            val starts = viewPoints.mapNotNull { element ->
                element.toRawChapterStart()
            }.sortedBy { it.startMs }
            if (starts.isEmpty()) return emptyList()

            return starts.mapIndexedNotNull { index, point ->
                val fallbackEnd = starts.getOrNull(index + 1)?.startMs
                    ?: durationMs.takeIf { it > 0L }
                    ?: point.endMs
                    ?: Long.MAX_VALUE
                val endMs = (point.endMs ?: fallbackEnd)
                    .coerceAtLeast(point.startMs)
                if (endMs <= point.startMs) return@mapIndexedNotNull null
                VideoProgressChapter(
                    startMs = point.startMs,
                    endMs = endMs,
                    title = point.title,
                    cover = point.cover,
                    type = point.type
                )
            }
        }
    }
}

fun List<VideoProgressChapter>.currentChapterAt(positionMs: Long): VideoProgressChapter? {
    if (isEmpty()) return null
    val safePosition = positionMs.coerceAtLeast(0L)
    return firstOrNull { chapter ->
        safePosition >= chapter.startMs && safePosition < chapter.endMs
    } ?: lastOrNull { safePosition >= it.startMs && safePosition <= it.endMs }
}

private data class RawChapterStart(
    val startMs: Long,
    val endMs: Long?,
    val title: String,
    val cover: String,
    val type: Int
)

private fun JsonElement.toRawChapterStart(): RawChapterStart? {
    val obj = runCatching { jsonObject }.getOrNull() ?: return null
    val start = obj.readTimeMs(
        TimeField("from", TimeUnit.Seconds),
        TimeField("start", TimeUnit.Seconds),
        TimeField("start_time", TimeUnit.Seconds),
        TimeField("startTime", TimeUnit.Milliseconds),
        TimeField("start_ms", TimeUnit.Milliseconds),
        TimeField("startMs", TimeUnit.Milliseconds)
    )
        ?: return null
    val title = obj.readString("content", "title", "name")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return RawChapterStart(
        startMs = start.coerceAtLeast(0L),
        endMs = obj.readTimeMs(
            TimeField("to", TimeUnit.Seconds),
            TimeField("end", TimeUnit.Seconds),
            TimeField("end_time", TimeUnit.Seconds),
            TimeField("endTime", TimeUnit.Milliseconds),
            TimeField("end_ms", TimeUnit.Milliseconds),
            TimeField("endMs", TimeUnit.Milliseconds)
        ),
        title = title,
        cover = obj.readString("cover", "imgUrl", "img_url", "image").orEmpty(),
        type = obj.readInt("type") ?: 0
    )
}

private enum class TimeUnit {
    Seconds,
    Milliseconds
}

private data class TimeField(
    val key: String,
    val unit: TimeUnit
)

private fun Map<String, JsonElement>.readString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.contentOrNull
    }
}

private fun Map<String, JsonElement>.readInt(vararg keys: String): Int? {
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.intOrNull
    }
}

private fun Map<String, JsonElement>.readTimeMs(vararg fields: TimeField): Long? {
    val field = fields.firstNotNullOfOrNull { field ->
        get(field.key)?.jsonPrimitive?.doubleOrNull?.let { field to it }
    } ?: return null
    return when (field.first.unit) {
        TimeUnit.Seconds -> (field.second * 1000).toLong()
        TimeUnit.Milliseconds -> field.second.toLong()
    }
}
