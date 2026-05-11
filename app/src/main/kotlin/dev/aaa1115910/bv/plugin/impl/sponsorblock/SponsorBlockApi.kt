package dev.aaa1115910.bv.plugin.impl.sponsorblock

interface SponsorBlockApi {
    suspend fun getSegments(bvid: String): List<SponsorSegment>
}
