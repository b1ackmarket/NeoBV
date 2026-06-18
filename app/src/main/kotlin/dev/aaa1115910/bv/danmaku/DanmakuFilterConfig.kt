package dev.aaa1115910.bv.danmaku

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuFilterRuleData
import dev.aaa1115910.bv.util.Prefs
import kotlinx.serialization.Serializable

@Serializable
data class DanmakuFilterConfig(
    val enabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val localKeywords: String = "",
    val localRegexes: String = "",
    val localUserHashes: String = "",
    val deduplicateEnabled: Boolean = false,
    val deduplicateThreshold: Int = 5,
    val deduplicateMergeDiffType: Boolean = false,
    val deduplicatePassSubtitle: Boolean = true,
    val deduplicatePassSpecial: Boolean = true,
    val deduplicatePassBottom: Boolean = true,
    val deduplicatePassTop: Boolean = true
)

enum class DanmakuFilterRuleType {
    Keyword,
    Regex,
    User
}

data class DanmakuFilterRule(
    val type: DanmakuFilterRuleType,
    val value: String,
    val source: String = "local"
)

data class DanmakuFilterSummary(
    val cloudRuleCount: Int,
    val localKeywordCount: Int,
    val localRegexCount: Int,
    val localUserCount: Int
)

fun readDanmakuFilterConfigFromPrefs(): DanmakuFilterConfig {
    return DanmakuFilterConfig(
        enabled = Prefs.enableDanmakuFilterWebConfig && Prefs.enableDanmakuFilter,
        caseSensitive = Prefs.danmakuFilterCaseSensitive,
        localKeywords = Prefs.localDanmakuFilterKeywords,
        localRegexes = Prefs.localDanmakuFilterRegexes,
        localUserHashes = Prefs.localDanmakuFilterUserHashes,
        deduplicateEnabled = Prefs.enableDanmakuFilterWebConfig && Prefs.danmakuFilterDeduplicateEnabled,
        deduplicateThreshold = Prefs.danmakuFilterDeduplicateThreshold,
        deduplicateMergeDiffType = Prefs.danmakuFilterDeduplicateMergeDiffType,
        deduplicatePassSubtitle = Prefs.danmakuFilterDeduplicatePassSubtitle,
        deduplicatePassSpecial = Prefs.danmakuFilterDeduplicatePassSpecial,
        deduplicatePassBottom = Prefs.danmakuFilterDeduplicatePassBottom,
        deduplicatePassTop = Prefs.danmakuFilterDeduplicatePassTop
    )
}

fun writeDanmakuFilterConfigToPrefs(config: DanmakuFilterConfig) {
    Prefs.enableDanmakuFilter = config.enabled
    Prefs.danmakuFilterCaseSensitive = config.caseSensitive
    Prefs.localDanmakuFilterKeywords = config.localKeywords
    Prefs.localDanmakuFilterRegexes = config.localRegexes
    Prefs.localDanmakuFilterUserHashes = config.localUserHashes
    Prefs.danmakuFilterDeduplicateEnabled = config.deduplicateEnabled
    Prefs.danmakuFilterDeduplicateThreshold = config.deduplicateThreshold
    Prefs.danmakuFilterDeduplicateMergeDiffType = config.deduplicateMergeDiffType
    Prefs.danmakuFilterDeduplicatePassSubtitle = config.deduplicatePassSubtitle
    Prefs.danmakuFilterDeduplicatePassSpecial = config.deduplicatePassSpecial
    Prefs.danmakuFilterDeduplicatePassBottom = config.deduplicatePassBottom
    Prefs.danmakuFilterDeduplicatePassTop = config.deduplicatePassTop
}

fun buildDanmakuFilterRules(
    config: DanmakuFilterConfig
): List<DanmakuFilterRule> {
    if (!config.enabled) return emptyList()
    return buildList {
        config.localKeywords.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.Keyword, it, source = "local"))
        }
        config.localRegexes.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.Regex, it, source = "local"))
        }
        config.localUserHashes.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.User, it, source = "local"))
        }
    }.distinctBy { it.type to it.value.lowercase() }
}

fun summarizeDanmakuFilterRules(
    config: DanmakuFilterConfig
): DanmakuFilterSummary {
    return DanmakuFilterSummary(
        cloudRuleCount = 0,
        localKeywordCount = config.localKeywords.toRuleLines().size,
        localRegexCount = config.localRegexes.toRuleLines().size,
        localUserCount = config.localUserHashes.toRuleLines().size
    )
}

fun shouldFetchCloudDanmakuFilterRules(nowMs: Long = System.currentTimeMillis()): Boolean {
    val uid = currentDanmakuFilterUid() ?: return false
    if (Prefs.cloudDanmakuFilterUid != uid) return true
    val lastSyncAt = Prefs.cloudDanmakuFilterSyncedAt
    return lastSyncAt <= 0L || nowMs - lastSyncAt >= CloudDanmakuFilterRefreshIntervalMs
}

fun cacheCloudDanmakuFilterRules(
    uid: Long,
    rules: List<DanmakuFilterRuleData>,
    nowMs: Long = System.currentTimeMillis()
) {
    val activeRules = rules.mapNotNull(DanmakuFilterRuleData::toDanmakuFilterRule)
    Prefs.cloudDanmakuFilterUid = uid
    Prefs.cloudDanmakuFilterSyncedAt = nowMs
    Prefs.cloudDanmakuFilterKeywords = activeRules
        .filter { it.type == DanmakuFilterRuleType.Keyword }
        .joinToString("\n") { it.value }
        .trimLineList()
    Prefs.cloudDanmakuFilterRegexes = activeRules
        .filter { it.type == DanmakuFilterRuleType.Regex }
        .joinToString("\n") { it.value }
        .trimLineList()
    Prefs.cloudDanmakuFilterUserHashes = activeRules
        .filter { it.type == DanmakuFilterRuleType.User }
        .joinToString("\n") { it.value }
        .trimLineList()
}

fun currentDanmakuFilterUid(): Long? {
    return Prefs.uid.takeIf { Prefs.isLogin && Prefs.sessData.isNotBlank() && it > 0L }
}

class DanmakuFilterMatcher(
    private val config: DanmakuFilterConfig,
    rules: List<DanmakuFilterRule>
) {
    private val keywordRules = rules
        .filter { it.type == DanmakuFilterRuleType.Keyword }
        .map { if (config.caseSensitive) it.value else it.value.lowercase() }
        .filter { it.isNotBlank() }

    private val regexRules = rules
        .filter { it.type == DanmakuFilterRuleType.Regex }
        .mapNotNull { rule ->
            runCatching {
                if (config.caseSensitive) Regex(rule.value) else Regex(rule.value, RegexOption.IGNORE_CASE)
            }.getOrNull()
        }

    private val userHashes = rules
        .filter { it.type == DanmakuFilterRuleType.User }
        .map { it.value.lowercase() }
        .filter { it.isNotBlank() }
        .toSet()

    fun blocks(text: String, midHash: String): Boolean {
        if (!config.enabled) return false
        if (userHashes.contains(midHash.lowercase())) return true

        val targetText = if (config.caseSensitive) text else text.lowercase()
        if (keywordRules.any { targetText.contains(it) }) return true
        return regexRules.any { it.containsMatchIn(text) }
    }

    fun blocks(data: DanmakuData): Boolean {
        return blocks(data.text, data.midHash)
    }
}

private fun DanmakuFilterRuleData.toDanmakuFilterRule(): DanmakuFilterRule? {
    if (isDeleted || filter.isBlank()) return null
    val ruleType = when (type) {
        0 -> DanmakuFilterRuleType.Keyword
        1 -> DanmakuFilterRuleType.Regex
        2 -> DanmakuFilterRuleType.User
        else -> return null
    }
    return DanmakuFilterRule(
        type = ruleType,
        value = filter.trim(),
        source = "cloud"
    )
}

private data class CachedCloudDanmakuFilterLines(
    val keywords: String = "",
    val regexes: String = "",
    val userHashes: String = ""
)

private fun currentAccountCachedCloudLines(): CachedCloudDanmakuFilterLines {
    val uid = currentDanmakuFilterUid() ?: return CachedCloudDanmakuFilterLines()
    if (Prefs.cloudDanmakuFilterUid != uid) return CachedCloudDanmakuFilterLines()
    return CachedCloudDanmakuFilterLines(
        keywords = Prefs.cloudDanmakuFilterKeywords,
        regexes = Prefs.cloudDanmakuFilterRegexes,
        userHashes = Prefs.cloudDanmakuFilterUserHashes
    )
}

fun String.toRuleLines(): List<String> {
    return lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(500)
        .toList()
}

private fun String.trimLineList(): String = toRuleLines().joinToString("\n")

fun mergeLineLists(vararg values: String): String {
    return values.flatMap { it.toRuleLines() }
        .distinctBy { it.lowercase() }
        .joinToString("\n")
}

private fun subtractLineList(value: String, subtract: String): String {
    val blocked = subtract.toRuleLines().map { it.lowercase() }.toSet()
    return value.toRuleLines()
        .filterNot { it.lowercase() in blocked }
        .joinToString("\n")
}

private const val CloudDanmakuFilterRefreshIntervalMs = 6L * 60L * 60L * 1000L

data class DeduplicateDanmaku(
    val raw: Any,
    val text: String,
    val positionMs: Long,
    val mode: Int,
    val pool: Int
)

/**
 * 弹幕合并去重算法 (100% 自主手写干净实现，完全规避开源协议风险)
 * 仅用于 NeoBV 内部进行重复弹幕的折叠拦截。
 */
fun filterDeduplicateInternal(
    danmakus: List<DeduplicateDanmaku>,
    config: DanmakuFilterConfig
): List<DeduplicateDanmaku> {
    val thresholdMs = config.deduplicateThreshold * 1000L
    if (thresholdMs <= 0) return danmakus
 
    val sorted = danmakus.sortedBy { it.positionMs }
    val result = mutableListOf<DeduplicateDanmaku>()
    val lastRetainedMap = mutableMapOf<String, DeduplicateDanmaku>()
 
    for (d in sorted) {
        val isSubtitle = d.pool == 1
        val isSpecial = d.mode == 7 || d.mode == 8 || d.mode == 9 || d.pool == 2
        val isBottom = d.mode == 4
        val isTop = d.mode == 5
 
        if ((config.deduplicatePassSubtitle && isSubtitle) ||
            (config.deduplicatePassSpecial && isSpecial) ||
            (config.deduplicatePassBottom && isBottom) ||
            (config.deduplicatePassTop && isTop)
        ) {
            result.add(d)
            continue
        }
 
        val textKey = if (config.caseSensitive) d.text.trim() else d.text.trim().lowercase()
        val lastRetained = lastRetainedMap[textKey]
        if (lastRetained != null) {
            val timeDiff = d.positionMs - lastRetained.positionMs
            if (timeDiff <= thresholdMs) {
                val canMerge = if (config.deduplicateMergeDiffType) {
                    true
                } else {
                    d.mode == lastRetained.mode
                }
                if (canMerge) {
                    continue
                }
            }
        }
 
        result.add(d)
        lastRetainedMap[textKey] = d
    }
 
    return result
}

fun filterDanmakusWithDeduplicate(
    config: DanmakuFilterConfig,
    danmakus: List<bilibili.community.service.dm.v1.DanmakuElem>
): List<bilibili.community.service.dm.v1.DanmakuElem> {
    if (!config.deduplicateEnabled || config.deduplicateThreshold <= 0) return danmakus
    val deduplicateList = danmakus.map {
        DeduplicateDanmaku(
            raw = it,
            text = it.content,
            positionMs = it.progress.toLong(),
            mode = it.mode,
            pool = it.pool
        )
    }
    val filtered = filterDeduplicateInternal(deduplicateList, config)
    return filtered.map { it.raw as bilibili.community.service.dm.v1.DanmakuElem }
}

fun filterDanmakusWithDeduplicateXml(
    config: DanmakuFilterConfig,
    danmakus: List<DanmakuData>
): List<DanmakuData> {
    if (!config.deduplicateEnabled || config.deduplicateThreshold <= 0) return danmakus
    val deduplicateList = danmakus.map {
        DeduplicateDanmaku(
            raw = it,
            text = it.text,
            positionMs = (it.time * 1000).toLong(),
            mode = it.type,
            pool = it.pool
        )
    }
    val filtered = filterDeduplicateInternal(deduplicateList, config)
    return filtered.map { it.raw as DanmakuData }
}
