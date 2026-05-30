package dev.aaa1115910.bv.entity.carddata

data class VideoCardData(
    val avid: Long,
    val cid: Long? = null,
    val epId: Int? = null,
    val title: String,
    val cover: String,
    val upName: String,
    val upMid: Long? = null,
    val playString: String =  "",
    val danmakuString: String =  "",
    val timeString: String =  "",
    val jumpToSeason: Boolean = false,
    val pubTime: String? = null,
    val badges: List<String> = emptyList()
)
