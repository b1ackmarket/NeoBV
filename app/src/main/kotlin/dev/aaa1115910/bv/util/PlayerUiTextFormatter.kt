package dev.aaa1115910.bv.util

object PlayerUiTextFormatter {
    fun onlineCount(count: Int): String = onlineCount(count.toString())

    fun onlineCount(countText: String): String = "$countText 人一起看"

    fun playCount(count: Int): String = "${count.toWanString()}播放"
}
