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
    val rndTimeMs: Long? = null
) : LiveEvent

data class SuperChatEvent(
    val id: Long,
    val uid: Long,
    val username: String,
    val message: String,
    val price: Long,
    val eventTimeMs: Long = System.currentTimeMillis()
) : LiveEvent
