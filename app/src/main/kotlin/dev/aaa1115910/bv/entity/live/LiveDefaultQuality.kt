package dev.aaa1115910.bv.entity.live

enum class LiveDefaultQuality(
    val qn: Int,
    val displayName: String
) {
    Smooth(80, "360P流畅"),
    High(150, "480P高清"),
    Super(250, "720P超清"),
    BlueRay(400, "1080P蓝光"),
    Original(10000, "1080P原画"),
    HighBitrate(25000, "1080P高码率"),
    R2K(15000, "2K原画"),
    R4K(20000, "4K原画"),
    Dolby(30000, "杜比视界");

    companion object {
        fun fromQn(qn: Int): LiveDefaultQuality =
            entries.find { it.qn == qn } ?: Original
    }
}
