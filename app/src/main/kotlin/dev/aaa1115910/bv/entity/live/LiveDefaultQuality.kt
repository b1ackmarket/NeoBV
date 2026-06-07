package dev.aaa1115910.bv.entity.live

enum class LiveDefaultQuality(
    val qn: Int,
    val displayName: String
) {
    Smooth(80, "流畅"),
    High(150, "高清"),
    Super(250, "超清"),
    BlueRay(400, "蓝光"),
    Original(10000, "原画"),
    R2K(15000, "2K"),
    R4K(20000, "4K"),
    HighBitrate(25000, "原画真彩"),
    Dolby(30000, "杜比");

    companion object {
        fun fromQn(qn: Int): LiveDefaultQuality =
            entries.find { it.qn == qn } ?: Original
    }
}
