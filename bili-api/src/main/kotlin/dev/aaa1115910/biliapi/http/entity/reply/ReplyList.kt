package dev.aaa1115910.biliapi.http.entity.reply

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReplyData(
    val replies: List<ReplyItem>? = emptyList(),
    val top: TopReply? = null,
    val page: ReplyPage? = null
)

@Serializable
data class ReplyPage(
    val count: Int = 0
)

@Serializable
data class TopReply(
    val upper: ReplyItem? = null
)

@Serializable
data class ReplyItem(
    val rpid: Long = 0L,
    val oid: Long = 0L,
    val type: Int = 0,
    val mid: Long = 0L,
    val ctime: Long = 0L,
    val like: Int = 0,
    val rcount: Int = 0,
    val member: ReplyMember = ReplyMember(),
    val content: ReplyContent = ReplyContent(),
    @SerialName("reply_control")
    val replyControl: ReplyControl? = null
)

@Serializable
data class ReplyMember(
    val mid: String = "",
    val uname: String = "",
    val avatar: String = "",
    val vip: ReplyVip? = null
)

@Serializable
data class ReplyVip(
    @SerialName("nickname_color")
    val nicknameColor: String = ""
)

@Serializable
data class ReplyContent(
    val message: String = "",
    val pictures: List<ReplyPicture>? = emptyList(),
    val emote: Map<String, ReplyEmote>? = emptyMap()
)

@Serializable
data class ReplyPicture(
    @SerialName("img_src")
    val imgSrc: String = "",
    @SerialName("img_width")
    val imgWidth: Int = 0,
    @SerialName("img_height")
    val imgHeight: Int = 0
)

@Serializable
data class ReplyEmote(
    val id: Long = 0L,
    val text: String = "",
    val url: String = "",
    val size: Int = 1
)

@Serializable
data class ReplyControl(
    val location: String? = null
)
