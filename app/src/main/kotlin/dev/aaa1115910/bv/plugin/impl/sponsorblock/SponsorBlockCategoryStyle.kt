package dev.aaa1115910.bv.plugin.impl.sponsorblock

import dev.aaa1115910.bv.entity.ProgressSegmentMark

object SponsorBlockCategoryStyle {
    private val categoryColors = linkedMapOf(
        "sponsor" to 0xFF43D676,
        "selfpromo" to 0xFFFFE24D,
        "exclusive_access" to 0xFF1FD2A4,
        "interaction" to 0xFFD94BFF,
        "poi_highlight" to 0xFFFF4AA5,
        "intro" to 0xFF22D6FF,
        "outro" to 0xFF1547FF,
        "preview" to 0xFF33A3FF,
        "filler" to 0xFF8F96A3,
        "music_offtopic" to 0xFFFFAB1F
    )

    fun colorArgb(category: String): Long? = categoryColors[category]

    fun cssDotRules(): String = categoryColors.entries.joinToString("\n") { (category, colorArgb) ->
        ".dot-$category { background: ${colorArgb.toCssHex()}; }"
    }

    private fun Long.toCssHex(): String = "#%06X".format(this and 0x00FFFFFF)
}

fun buildSponsorBlockProgressMarks(
    config: SponsorBlockConfig,
    segments: List<SponsorSegment>
): List<ProgressSegmentMark> {
    if (!config.enabled) return emptyList()

    return segments.mapNotNull { segment ->
        val policy = config.categoryPolicy[segment.category] ?: SkipPolicy.Disabled
        if (policy != SkipPolicy.Auto && policy != SkipPolicy.Prompt) return@mapNotNull null
        val colorArgb = SponsorBlockCategoryStyle.colorArgb(segment.category) ?: return@mapNotNull null
        val startMs = segment.startMs.coerceAtLeast(0L)
        val endMs = segment.endMs.coerceAtLeast(0L)
        if (endMs <= startMs) return@mapNotNull null

        ProgressSegmentMark(
            startMs = startMs,
            endMs = endMs,
            colorArgb = colorArgb
        )
    }
}
