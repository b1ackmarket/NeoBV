package dev.aaa1115910.bv.entity.live

enum class LiveDefaultQuality(
    val qn: Int,
    val displayName: String
) {
    Original(10000, "1080P原画"),
    HighBitrate(25000, "1080P高码率"),
    BlueRay(400, "1080P蓝光"),
    Super(250, "720P超清");

    companion object {
        fun fromQn(qn: Int): LiveDefaultQuality =
            entries.find { it.qn == qn } ?: Original
    }
}
