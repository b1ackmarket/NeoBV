package dev.aaa1115910.bv.plugin.impl.sponsorblock

data class SponsorSegment(
    val id: String,
    val category: String,
    val startMs: Long,
    val endMs: Long
) {
    fun contains(positionMs: Long): Boolean = positionMs in startMs..endMs
}

enum class SkipPolicy {
    Auto,
    Prompt,
    Disabled
}
