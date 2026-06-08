package dev.aaa1115910.bv.cast.protocol

data class CastContent(
    val aid: Long? = null,
    val bvid: String? = null,
    val cid: Long? = null,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val roomId: Int? = null,
    val seekSeconds: Int? = null,
    val quality: Int? = null,
    val playSpeed: Float? = null,
    val danmakuEnabled: Boolean? = null,
    val title: String? = null,
    val partTitle: String? = null,
    val directMediaUrl: String? = null,
    val rawFields: Map<String, String> = emptyMap()
) {
    val hasVideoIdentity: Boolean
        get() = aid != null || !bvid.isNullOrBlank() || epid != null || seasonId != null

    val hasLiveIdentity: Boolean
        get() = roomId != null && roomId > 0

    val hasDirectMedia: Boolean
        get() = !directMediaUrl.isNullOrBlank()
}
