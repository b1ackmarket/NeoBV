package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.entity.live.LiveRoomCard
import org.koin.core.annotation.Single

data class LiveJumpModeQueueItem(
    val roomId: Int,
    val title: String,
    val upName: String,
    val online: Int
)

data class LiveJumpModeQueue(
    val selectedRoomId: Int,
    val items: List<LiveJumpModeQueueItem>
) {
    val selectedIndex: Int
        get() = items.indexOfFirst { it.roomId == selectedRoomId }

    val isUsable: Boolean
        get() = items.size > 1 && selectedIndex != -1

    fun copyForRoom(roomId: Int): LiveJumpModeQueue {
        return copy(selectedRoomId = roomId)
    }
}

@Single
class LiveJumpModeRepository {
    private var pendingQueue: LiveJumpModeQueue? = null

    fun setPendingQueue(
        selectedRoomId: Int,
        items: List<LiveJumpModeQueueItem>
    ) {
        pendingQueue = LiveJumpModeQueue(
            selectedRoomId = selectedRoomId,
            items = items
        ).takeIf { it.isUsable }
    }

    fun clearPendingQueue() {
        pendingQueue = null
    }

    fun consumeQueueFor(roomId: Int): LiveJumpModeQueue? {
        val queue = pendingQueue?.takeIf { queue ->
            queue.selectedRoomId == roomId && queue.isUsable
        }
        pendingQueue = null
        return queue
    }
}

fun List<LiveRoomCard>.toLiveJumpModeItems(): List<LiveJumpModeQueueItem> {
    return filter { it.roomId > 0 }
        .distinctBy { it.roomId }
        .map { room ->
            LiveJumpModeQueueItem(
                roomId = room.roomId,
                title = room.title,
                upName = room.upName,
                online = room.online
            )
        }
}
