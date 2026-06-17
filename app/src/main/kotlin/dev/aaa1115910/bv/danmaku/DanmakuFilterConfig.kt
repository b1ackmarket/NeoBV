package dev.aaa1115910.bv.danmaku

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuFilterRuleData
import dev.aaa1115910.bv.util.Prefs
import kotlinx.serialization.Serializable

@Serializable
data class DanmakuFilterConfig(
    val enabled: Boolean = true,
    val syncCloudRules: Boolean = true,
    val caseSensitive: Boolean = false,
    val localKeywords: String = "",
    val localRegexes: String = "",
    val localUserHashes: String = ""
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
    val syncCloudRules = Prefs.syncCloudDanmakuFilter
    val cloudRules = if (syncCloudRules) {
        currentAccountCachedCloudLines()
    } else {
        CachedCloudDanmakuFilterLines()
    }
    return DanmakuFilterConfig(
        enabled = Prefs.enableDanmakuFilterWebConfig && Prefs.enableDanmakuFilter,
        syncCloudRules = syncCloudRules,
        caseSensitive = Prefs.danmakuFilterCaseSensitive,
        localKeywords = mergeLineLists(Prefs.localDanmakuFilterKeywords, cloudRules.keywords),
        localRegexes = mergeLineLists(Prefs.localDanmakuFilterRegexes, cloudRules.regexes),
        localUserHashes = mergeLineLists(Prefs.localDanmakuFilterUserHashes, cloudRules.userHashes)
    )
}

fun writeDanmakuFilterConfigToPrefs(config: DanmakuFilterConfig) {
    val cloudRules = currentAccountCachedCloudLines()
    Prefs.enableDanmakuFilter = config.enabled
    Prefs.syncCloudDanmakuFilter = config.syncCloudRules
    Prefs.danmakuFilterCaseSensitive = config.caseSensitive
    Prefs.localDanmakuFilterKeywords = subtractLineList(config.localKeywords, cloudRules.keywords)
    Prefs.localDanmakuFilterRegexes = subtractLineList(config.localRegexes, cloudRules.regexes)
    Prefs.localDanmakuFilterUserHashes = subtractLineList(config.localUserHashes, cloudRules.userHashes)
}

fun buildDanmakuFilterRules(
    config: DanmakuFilterConfig,
    cloudRules: List<DanmakuFilterRuleData>
): List<DanmakuFilterRule> {
    if (!config.enabled) return emptyList()
    val localRules = buildList {
        config.localKeywords.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.Keyword, it, source = "local"))
        }
        config.localRegexes.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.Regex, it, source = "local"))
        }
        config.localUserHashes.toRuleLines().forEach {
            add(DanmakuFilterRule(DanmakuFilterRuleType.User, it, source = "local"))
        }
    }
    val remoteRules = if (config.syncCloudRules) {
        cloudRules.mapNotNull(DanmakuFilterRuleData::toDanmakuFilterRule)
    } else {
        emptyList()
    }
    return (localRules + remoteRules).distinctBy { it.type to it.value.lowercase() }
}

fun summarizeDanmakuFilterRules(
    config: DanmakuFilterConfig,
    cloudRules: List<DanmakuFilterRuleData>
): DanmakuFilterSummary {
    return DanmakuFilterSummary(
        cloudRuleCount = cloudRules.count { it.toDanmakuFilterRule() != null },
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

private fun String.toRuleLines(): List<String> {
    return lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(500)
        .toList()
}

private fun String.trimLineList(): String = toRuleLines().joinToString("\n")

private fun mergeLineLists(vararg values: String): String {
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
