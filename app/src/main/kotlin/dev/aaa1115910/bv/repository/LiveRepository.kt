package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoData
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.bv.entity.live.LiveCategory
import dev.aaa1115910.bv.entity.live.LiveCategoryType
import dev.aaa1115910.bv.entity.live.LiveRoomCard
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.koin.core.annotation.Single

@Single
class LiveRepository(
    private val authRepository: AuthRepository
) {
    suspend fun resolveRoomContext(roomId: Int): LiveRoomContext {
        val roomInfo = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId).data
        val webInfo = runCatching {
            BiliLiveHttpApi.getLiveRoomWebInfo(roomId).data
        }.getOrNull()

        return webInfo?.toLiveRoomContext(
            fallbackRoomId = roomId,
            playInfo = roomInfo
        ) ?: LiveRoomContext(
            roomId = roomInfo?.roomId ?: roomId,
            ownerMid = roomInfo?.uid ?: 0L,
            isPortrait = roomInfo?.isPortrait ?: false,
            liveStatus = roomInfo?.liveStatus ?: 0
        )
    }

    suspend fun getHistoryDanmaku(roomId: Int): List<DanmakuEvent> {
        val history = BiliLiveHttpApi.getLiveDanmuHistory(roomId).data ?: return emptyList()
        return history.room.map { item ->
            DanmakuEvent(
                content = item.text,
                mid = item.uid,
                username = item.nickname,
                medalName = item.medal?.name,
                medalLevel = item.medal?.level,
                eventTimeMs = item.toEventTimeMs(),
                rndTimeMs = item.rnd.takeIf { it > 0L }?.let(::normalizeLiveTimestampMs)
            )
        }
    }

    suspend fun resolveRoomId(roomId: Int): Int {
        val roomInfo = BiliLiveHttpApi.getLiveRoomInfo(roomId).data
        return LiveStreamResolver.resolveRoomId(roomInfo, fallbackRoomId = roomId)
    }

    suspend fun getCategories(): List<LiveCategory> {
        val categories = mutableListOf(
            LiveCategory("following", "我的关注", LiveCategoryType.Following),
            LiveCategory("recommend", "推荐直播", LiveCategoryType.Recommend)
        )
        val response = BiliLiveHttpApi.getParentAreas()
        val areas = response.data ?: JsonArray(emptyList())
        categories.addAll(
            areas.mapNotNull { area ->
                val obj = area.asJsonObjectOrNull() ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val children = obj["list"].asJsonArrayOrNull()
                    ?.mapNotNull { child -> child.toLiveCategory(parentId = id) }
                    .orEmpty()
                val childrenWithAll = if (children.any { it.areaId == 0 }) {
                    children
                } else {
                    listOf(
                        LiveCategory(
                            key = "partition_${id}_all",
                            label = "全部$name",
                            type = LiveCategoryType.Partition,
                            parentAreaId = id,
                            areaId = 0
                        )
                    ) + children
                }
                LiveCategory(
                    key = "partition_$id",
                    label = name,
                    type = LiveCategoryType.Partition,
                    parentAreaId = id,
                    children = childrenWithAll
                )
            }
        )
        return categories
    }

    suspend fun getRooms(
        category: LiveCategory,
        page: Int = 1,
        pageSize: Int = 30
    ): List<LiveRoomCard> {
        return when (category.type) {
            LiveCategoryType.Following -> {
                val response = BiliLiveHttpApi.getFollowedLives(
                    page = page,
                    pageSize = pageSize,
                    sessData = authRepository.sessionData.orEmpty()
                )
                val list = response.data?.get("list").asJsonArrayOrNull() ?: JsonArray(emptyList())
                list.mapNotNull { it.toLiveRoomCard() }
            }

            LiveCategoryType.Recommend,
            LiveCategoryType.Partition -> {
                val response = BiliLiveHttpApi.getRecommendedLives(
                    page = page,
                    pageSize = pageSize,
                    parentId = category.parentAreaId ?: 0,
                    areaId = category.areaId
                )
                val data = response.data
                val list = when (data) {
                    is JsonArray -> data
                    is JsonObject -> data["list"].asJsonArrayOrNull() ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                list.mapNotNull { it.toLiveRoomCard() }
            }
        }
    }

    suspend fun resolvePlayableUrl(
        roomId: Int,
        qn: Int = 10000
    ): String? {
        return resolvePlayableSource(roomId = roomId, qn = qn)?.playUrl
    }

    suspend fun resolvePlayableSource(
        roomId: Int,
        qn: Int = 10000,
        lineIndex: Int = 0
    ): LivePlaybackSource? {
        val response = BiliLiveHttpApi.getLivePlayUrl(
            roomId = roomId,
            qn = qn,
            sessData = authRepository.sessionData.orEmpty()
        )
        val playInfo = response.data ?: return null
        return LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            requestedLineIndex = lineIndex
        )
    }
}

private fun JsonElement.toLiveRoomCard(): LiveRoomCard? {
    val obj = asJsonObjectOrNull() ?: return null
    val roomId = obj["roomid"]?.jsonPrimitive?.intOrNull
        ?: obj["room_id"]?.jsonPrimitive?.intOrNull
        ?: return null
    val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return null
    val cover = obj["pic"]?.jsonPrimitive?.content
        ?: obj["room_cover"]?.jsonPrimitive?.content
        ?: obj["cover"]?.jsonPrimitive?.content
        ?: obj["user_cover"]?.jsonPrimitive?.content
        ?: obj["keyframe"]?.jsonPrimitive?.content
        ?: ""
    val upName = obj["uname"]?.jsonPrimitive?.content ?: ""
    val online = obj["online"]?.jsonPrimitive?.intOrNull
        ?: obj["text_small"]?.jsonPrimitive?.content?.filter { it.isDigit() }?.toIntOrNull()
        ?: 0
    val areaName = obj["area_name"]?.jsonPrimitive?.contentOrNull
        ?: obj["areaName"]?.jsonPrimitive?.contentOrNull
        ?: obj["area_v2_name"]?.jsonPrimitive?.contentOrNull
        ?: ""
    return LiveRoomCard(
        roomId = roomId,
        title = title,
        cover = cover,
        upName = upName,
        online = online,
        areaName = areaName,
        badges = obj.extractLiveBadges()
    )
}

private fun JsonElement.toLiveCategory(parentId: Int): LiveCategory? {
    val obj = asJsonObjectOrNull() ?: return null
    val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null
    return LiveCategory(
        key = "partition_${parentId}_area_$id",
        label = name,
        type = LiveCategoryType.Partition,
        parentAreaId = parentId,
        areaId = id
    )
}

private fun JsonObject.extractLiveBadges(): List<String> {
    val badges = linkedSetOf<String>()
    get("pendent_info").asJsonObjectOrNull()
        ?.values
        ?.mapNotNull { pendant ->
            val obj = pendant.asJsonObjectOrNull() ?: return@mapNotNull null
            obj["content"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
        }
        ?.forEach { badges.add(it) }
    get("rank_name")?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?.let { badges.add(it) }
    get("watched_show").asJsonObjectOrNull()
        ?.get("text_large")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() && it.length <= 12 }
        ?.let { badges.add(it) }
    return badges.take(2)
}

internal fun JsonObject.toLiveRoomContext(
    fallbackRoomId: Int,
    playInfo: RoomPlayInfoData? = null
): LiveRoomContext {
    val roomInfo = this["room_info"].asJsonObjectOrNull()
    val roomTypes = roomInfo
        ?.get("room_type")
        .asJsonObjectOrNull()
        ?.keys
        .orEmpty()
        .toSet()
    val voiceMembers = this["multi_voice"]
        .asJsonObjectOrNull()
        ?.get("members")
        .asJsonArrayOrNull()
        ?.mapNotNull { memberElement ->
            val member = memberElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val nickname = member["nickname"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val avatar = member["avatar"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (nickname.isBlank() && avatar.isBlank()) return@mapNotNull null
            LiveVoiceRoomMember(
                nickname = nickname,
                avatar = avatar,
                priceText = member["price_text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isMute = member["is_mute"]?.jsonPrimitive?.intOrNull == 1 ||
                        member["is_mystery"]?.jsonPrimitive?.booleanOrNull == true
            )
        }
        .orEmpty()

    return LiveRoomContext(
        roomId = roomInfo?.get("room_id")?.jsonPrimitive?.intOrNull
            ?: playInfo?.roomId
            ?: fallbackRoomId,
        ownerMid = roomInfo?.get("uid")?.jsonPrimitive?.longOrNull
            ?: playInfo?.uid
            ?: 0L,
        isPortrait = playInfo?.isPortrait
            ?: (roomInfo?.get("live_screen_type")?.jsonPrimitive?.intOrNull == 1),
        liveStatus = roomInfo?.get("live_status")?.jsonPrimitive?.intOrNull
            ?: playInfo?.liveStatus
            ?: 0,
        title = roomInfo?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty(),
        cover = roomInfo?.get("cover")?.jsonPrimitive?.contentOrNull.orEmpty(),
        keyframe = roomInfo?.get("keyframe")?.jsonPrimitive?.contentOrNull.orEmpty(),
        areaName = roomInfo?.get("area_name")?.jsonPrimitive?.contentOrNull.orEmpty(),
        parentAreaName = roomInfo?.get("parent_area_name")?.jsonPrimitive?.contentOrNull.orEmpty(),
        roomTypeKeys = roomTypes,
        voiceMembers = voiceMembers
    )
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray

private fun dev.aaa1115910.biliapi.http.entity.live.HistoryDanmaku.HistoryDanmakuItem.toEventTimeMs(): Long {
    rnd.takeIf { it > 0L }?.let { return normalizeLiveTimestampMs(it) }
    return runCatching {
        val parsed = SimpleDateFormat("HH:mm:ss", Locale.US).parse(timeline) ?: return@runCatching System.currentTimeMillis()
        val parsedCalendar = Calendar.getInstance().apply { time = parsed }
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsedCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, parsedCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, parsedCalendar.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }.getOrDefault(System.currentTimeMillis())
}

private fun normalizeLiveTimestampMs(value: Long): Long {
    return if (value < 10_000_000_000L) value * 1000L else value
}
