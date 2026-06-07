package dev.aaa1115910.bv.repository

data class LiveRoomContext(
    val roomId: Int,
    val ownerMid: Long,
    val isPortrait: Boolean,
    val liveStatus: Int,
    val liveStartTime: Long = 0L,
    val title: String = "",
    val cover: String = "",
    val keyframe: String = "",
    val areaName: String = "",
    val parentAreaName: String = "",
    val roomTypeKeys: Set<String> = emptySet(),
    val voiceMembers: List<LiveVoiceRoomMember> = emptyList()
) {
    val isChatRoom: Boolean
        get() = parentAreaName == "聊天室" ||
                roomTypeKeys.any { it.endsWith("-71") } ||
                voiceMembers.isNotEmpty()
}

data class LiveVoiceRoomMember(
    val nickname: String,
    val avatar: String,
    val priceText: String,
    val isMute: Boolean
)
