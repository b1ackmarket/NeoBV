package dev.aaa1115910.bv.subtitle.translation

import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SubtitleTranslationManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val updateDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val providerFactory: (SubtitleTranslationConfig) -> SubtitleTranslationProvider = {
        SubtitleTranslationProviderFactory.create(it)
    }
) {
    private val cache = ConcurrentHashMap<String, String>()
    private val translatedIds = ConcurrentHashMap.newKeySet<Int>()
    private var job: Job? = null
    private var session = 0
    private var cachePrefix = ""

    fun reset(cachePrefix: String) {
        session += 1
        job?.cancel()
        job = null
        translatedIds.clear()
        this.cachePrefix = cachePrefix
    }

    fun clear() {
        session += 1
        job?.cancel()
        job = null
        translatedIds.clear()
        cachePrefix = ""
    }

    fun release() {
        clear()
        scope.cancel()
    }

    fun preload(
        sourceSubtitles: List<SubtitleItem>,
        currentTimeMs: Long,
        config: SubtitleTranslationConfig,
        title: String,
        aid: Long,
        cid: Long,
        subtitleId: Long,
        restartInFlight: Boolean = true,
        onUpdate: (List<SubtitleItem>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val sanitized = config.sanitized
        if (!sanitized.verified() || sourceSubtitles.isEmpty()) return
        val nextPrefix = buildCachePrefix(aid, cid, subtitleId, sanitized)
        if (nextPrefix != cachePrefix) reset(nextPrefix)
        if (!restartInFlight && job?.isActive == true) return
        val currentSession = session
        job?.cancel()
        job = scope.launch {
            runCatching {
                val provider = providerFactory(sanitized)
                val targetLanguage = SubtitleLanguages.find(sanitized.targetLanguage)
                val batches = sourceSubtitles
                    .priorityBatches(currentTimeMs, sanitized.preTranslateSeconds * 1000L, sanitized.requestBatchSize)
                for (batch in batches) {
                    if (currentSession != session) return@launch
                    val requestItems = batch.mapNotNull { index ->
                        if (translatedIds.contains(index)) return@mapNotNull null
                        val source = sourceSubtitles[index]
                        SubtitleTranslationItem(index, source.content)
                    }
                    if (requestItems.isEmpty()) continue
                    val context = buildContext(sourceSubtitles, batch.first(), sanitized.contextBefore, sanitized.contextAfter)
                    val result = provider.translate(
                        SubtitleTranslationRequest(
                            title = title,
                            targetLanguage = targetLanguage,
                            context = context,
                            items = requestItems
                        )
                    )
                    if (currentSession != session) return@launch
                    result.translations.forEach { (index, text) ->
                        if (index in sourceSubtitles.indices) {
                            cache["$cachePrefix:$index"] = text
                            translatedIds.add(index)
                        }
                    }
                    withContext(updateDispatcher) {
                        if (currentSession == session) onUpdate(buildTranslatedSubtitles(sourceSubtitles))
                    }
                }
            }.onFailure { error ->
                if (currentSession == session) withContext(updateDispatcher) { onError(error) }
            }
        }
    }

    fun buildTranslatedSubtitles(sourceSubtitles: List<SubtitleItem>): List<SubtitleItem> {
        if (sourceSubtitles.isEmpty()) return emptyList()
        return sourceSubtitles.mapIndexedNotNull { index, source ->
            val text = cache["$cachePrefix:$index"].orEmpty()
            if (text.isBlank()) null else source.copy(content = text)
        }
    }
}

fun buildCachePrefix(
    aid: Long,
    cid: Long,
    subtitleId: Long,
    config: SubtitleTranslationConfig
): String {
    return listOf(
        aid,
        cid,
        subtitleId,
        config.providerType.name,
        config.targetLanguage,
        config.configSignature()
    ).joinToString(":")
}

internal fun List<SubtitleItem>.priorityBatches(
    currentTimeMs: Long,
    preTranslateMs: Long,
    batchSize: Int
): List<List<Int>> {
    if (isEmpty()) return emptyList()
    val safeBatchSize = batchSize.coerceIn(1, 80)
    val startIndex = indexOfFirst { it.to.totalMills >= currentTimeMs }.takeIf { it >= 0 } ?: 0
    val endTime = currentTimeMs + preTranslateMs.coerceAtLeast(0L)
    val priority = indices.drop(startIndex).takeWhile { this[it].from.totalMills <= endTime }
    val prioritySet = priority.toSet()
    val rest = indices.filterNot { it in prioritySet }
    return priority.chunked(safeBatchSize) + rest.chunked(safeBatchSize)
}

internal fun buildContext(
    sourceSubtitles: List<SubtitleItem>,
    startIndex: Int,
    contextBefore: Int,
    contextAfter: Int
): String {
    if (sourceSubtitles.isEmpty()) return ""
    val start = (startIndex - contextBefore.coerceAtLeast(0)).coerceAtLeast(0)
    val end = (startIndex + contextAfter.coerceAtLeast(0)).coerceAtMost(sourceSubtitles.lastIndex)
    return (start..end).joinToString("\n") { index -> sourceSubtitles[index].content }
}
