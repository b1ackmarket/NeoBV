package dev.aaa1115910.bv.viewmodel.player

data class StreamCandidate(
    val quality: Int,
    val codecPrefix: String,
    val videoUrl: String,
    val audioUrl: String?,
)

class PlaybackFallbackPlanner(
    private val candidates: List<StreamCandidate>
) {
    fun selectPreferred(quality: Int, codecPrefix: String): StreamCandidate? {
        return candidates.firstOrNull { it.quality == quality && it.codecPrefix == codecPrefix }
            ?: candidates.firstOrNull { it.quality == quality }
            ?: candidates.maxByOrNull { it.quality }
    }

    fun nextAfterFailure(current: StreamCandidate): StreamCandidate? {
        val sameQuality = candidates.filter { it.quality == current.quality }
        val currentIndex = sameQuality.indexOfFirst { it == current }
        if (currentIndex != -1 && currentIndex + 1 < sameQuality.size) {
            return sameQuality[currentIndex + 1]
        }

        return candidates
            .filter { it.quality < current.quality }
            .maxByOrNull { it.quality }
    }
}
