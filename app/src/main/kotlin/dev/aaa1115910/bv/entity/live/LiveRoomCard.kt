package dev.aaa1115910.bv.entity.live

import android.content.Context
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.component.TopNavItem

data class LiveRoomCard(
    val roomId: Int,
    val title: String,
    val cover: String,
    val upName: String,
    val online: Int,
    val areaName: String = ""
)

data class LiveCategory(
    val key: String,
    val label: String,
    val type: LiveCategoryType,
    val parentAreaId: Int? = null,
    val areaId: Int = 0
) : TopNavItem {
    override fun getDisplayName(context: Context): String = label
}

enum class LiveCategoryType {
    Following,
    Recommend,
    Partition
}
