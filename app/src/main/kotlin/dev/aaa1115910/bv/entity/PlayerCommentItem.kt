package dev.aaa1115910.bv.entity

data class PlayerCommentItem(
    val id: String,
    val mid: Long = 0L,
    val username: String,
    val avatar: String = "",
    val message: String,
    val emotes: List<PlayerCommentEmote> = emptyList(),
    val pictures: List<PlayerCommentPicture> = emptyList(),
    val timeText: String = "",
    val likeText: String = "",
    val replyText: String = "",
    val ipLocation: String = "",
    val badgeText: String? = null,
    val color: Int? = null
)

data class PlayerCommentEmote(
    val text: String,
    val url: String,
    val size: Int = 1
)

data class PlayerCommentPicture(
    val url: String,
    val width: Int = 0,
    val height: Int = 0
)

enum class PlayerCommentSort {
    Latest,
    Hot
}
