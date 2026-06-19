package dev.aaa1115910.biliapi.http.entity.live

interface LiveEvent

data class DanmakuEvent(
    val content: String,
    val mid: Long,
    val username: String,
    val medalName: String? = null,
    val medalLevel: Int? = null,
    val color: Int = 0xffffff,
    val mode: Int = 1,
    val eventTimeMs: Long = System.currentTimeMillis(),
    val sendTimeMs: Long? = null,
    val rndTimeMs: Long? = null,
    val emoticonUrl: String? = null,
    val rawJson: String? = null,
    val userFace: String? = null,
    val emotes: Map<String, String> = emptyMap()
) : LiveEvent

data class SuperChatEvent(
    val id: Long,
    val uid: Long,
    val username: String,
    val message: String,
    val price: Long,
    val eventTimeMs: Long = System.currentTimeMillis()
) : LiveEvent

data class InteractEvent(
    val uid: Long,
    val username: String,
    val action: Int,
    val actionText: String,
    val avatar: String? = null
) : LiveEvent

data class GiftEvent(
    val uid: Long,
    val username: String,
    val giftName: String,
    val num: Int,
    val action: String
) : LiveEvent

data class GuardBuyEvent(
    val uid: Long,
    val username: String,
    val guardLevel: Int,
    val guardName: String,
    val num: Int
) : LiveEvent

data class LikeEvent(
    val uid: Long,
    val username: String,
    val likeText: String
) : LiveEvent

data class EntryEffectEvent(
    val uid: Long,
    val username: String,
    val entryText: String
) : LiveEvent

data class ComboSendEvent(
    val uid: Long,
    val username: String,
    val giftName: String,
    val comboNum: Int,
    val action: String
) : LiveEvent
