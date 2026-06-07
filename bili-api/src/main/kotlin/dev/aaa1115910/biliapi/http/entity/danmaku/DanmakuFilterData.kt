package dev.aaa1115910.biliapi.http.entity.danmaku

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DanmakuFilterData(
    val rule: List<DanmakuFilterRuleData> = emptyList(),
    val toast: String = "",
    val valid: Boolean = true,
    val ver: Long = 0L
)

@Serializable
data class DanmakuFilterRuleData(
    val id: Long = 0L,
    val type: Int = 0,
    val filter: String = "",
    val comment: String = "",
    val ctime: Long = 0L,
    val mtime: Long = 0L,
    val mid: Long = 0L,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
)
