package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import org.koin.core.annotation.Single

enum class JumpModeSource {
    Home,
    Search,
    Personal
}

data class JumpModeQueueItem(
    val aid: Long,
    val title: String,
    val cid: Long? = null
)

data class JumpModeQueue(
    val source: JumpModeSource,
    val selectedAid: Long,
    val items: List<JumpModeQueueItem>
) {
    val selectedIndex: Int
        get() = items.indexOfFirst { it.aid == selectedAid }

    val isUsable: Boolean
        get() = items.size > 1 && selectedIndex != -1
}

@Single
class JumpModeRepository {
    private var pendingQueue: JumpModeQueue? = null

    fun setPendingQueue(queue: JumpModeQueue?) {
        pendingQueue = queue?.takeIf { it.isUsable }
    }

    fun setPendingQueue(
        source: JumpModeSource,
        selectedAid: Long,
        items: List<JumpModeQueueItem>
    ) {
        setPendingQueue(
            JumpModeQueue(
                source = source,
                selectedAid = selectedAid,
                items = items
            )
        )
    }

    fun clearPendingQueue() {
        pendingQueue = null
    }

    fun consumeQueueFor(aid: Long): JumpModeQueue? {
        val queue = pendingQueue?.takeIf { queue ->
            queue.selectedAid == aid && queue.isUsable
        }
        pendingQueue = null
        return queue
    }
}

@JvmName("videoCardDataListToJumpModeItems")
fun List<VideoCardData>.toJumpModeItems(): List<JumpModeQueueItem> {
    return filter { it.avid > 0 && it.epId == null && !it.jumpToSeason }
        .map { item ->
            JumpModeQueueItem(
                aid = item.avid,
                title = item.title,
                cid = item.cid?.takeIf { cid -> cid > 0 }
            )
        }
}

@JvmName("ugcItemListToJumpModeItems")
fun List<UgcItem>.toJumpModeItems(): List<JumpModeQueueItem> {
    return filter { it.aid > 0 }
        .map { item ->
            JumpModeQueueItem(
                aid = item.aid,
                title = item.title
            )
        }
}
