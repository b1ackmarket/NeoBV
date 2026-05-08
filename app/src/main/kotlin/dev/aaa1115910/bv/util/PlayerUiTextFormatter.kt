package dev.aaa1115910.bv.util

object PlayerUiTextFormatter {
    fun onlineCount(count: Int): String = "$count 人正在看"

    fun playCount(count: Int): String = "${count.toWanString()}播放"
}
