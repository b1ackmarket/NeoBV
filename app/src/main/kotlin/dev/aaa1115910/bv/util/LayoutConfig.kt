package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.PgcTopNavItem
import dev.aaa1115910.bv.entity.live.LiveCategory
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class LayoutConfigGroup(val id: String, val displayName: String) {
    LeftNav("leftNav", "左侧导航"),
    Home("home", "首页顶部"),
    Pgc("pgc", "影视顶部"),
    Personal("personal", "个人顶部"),
    Live("live", "直播顶部"),
    PlayerBottomOsd("playerBottomOsd", "播放器底部 OSD"),
    LivePlayerBottomOsd("livePlayerBottomOsd", "直播播放器底部 OSD")
}

enum class PlayerBottomOsdControl(val id: String, val label: String) {
    VideoList("videoList", "选集"),
    Danmaku("danmaku", "弹幕开关"),
    Subtitle("subtitle", "字幕"),
    JumpMode("jumpMode", "跳动模式"),
    VideoInfo("videoInfo", "视频信息"),
    UpPage("upPage", "UP主页"),
    RelatedVideos("relatedVideos", "相关视频"),
    Comments("comments", "评论"),
    Loop("loop", "循环播放"),
    Settings("settings", "播放设置")
}

enum class LiveBottomOsdControl(val id: String, val label: String) {
    PlayPause("playPause", "播放暂停"),
    Refresh("refresh", "刷新"),
    Danmaku("danmaku", "弹幕开关"),
    JumpMode("jumpMode", "跳动模式"),
    Comments("comments", "评论"),
    UpPage("upPage", "UP主页"),
    Settings("settings", "直播设置")
}

@Serializable
data class LayoutConfigItem(
    val id: String,
    val label: String,
    val hidden: Boolean = false
)

@Serializable
data class LayoutConfigState(
    val groups: Map<String, List<LayoutConfigItem>> = emptyMap()
)

object LayoutConfig {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun read(): LayoutConfigState {
        return decode(Prefs.layoutConfigJson)
    }

    fun write(state: LayoutConfigState) {
        Prefs.layoutConfigJson = json.encodeToString(normalize(state))
    }

    fun reset() {
        Prefs.layoutConfigJson = ""
    }

    fun decode(value: String): LayoutConfigState {
        if (value.isBlank()) return LayoutConfigState()
        return runCatching { json.decodeFromString<LayoutConfigState>(value) }
            .getOrDefault(LayoutConfigState())
            .let(::normalize)
    }

    fun normalize(state: LayoutConfigState): LayoutConfigState {
        val defaults = defaultGroups()
        val merged = defaults.mapValues { (groupId, defaultItems) ->
            val saved = state.groups[groupId].orEmpty()
            mergeItems(defaultItems, saved)
        }
        return LayoutConfigState(merged)
    }

    fun defaultGroups(): Map<String, List<LayoutConfigItem>> {
        return mapOf(
            LayoutConfigGroup.LeftNav.id to LeftNaviItem.entries.map { LayoutConfigItem(it.name, it.displayName) },
            LayoutConfigGroup.Home.id to HomeTopNavItem.entries.map { LayoutConfigItem(it.name, it.layoutLabel) },
            LayoutConfigGroup.Pgc.id to PgcTopNavItem.entries.map { LayoutConfigItem(it.name, it.layoutLabel) },
            LayoutConfigGroup.Personal.id to PersonalTopNavItem.entries.map { LayoutConfigItem(it.name, it.layoutLabel) },
            LayoutConfigGroup.Live.id to defaultLiveItems(),
            LayoutConfigGroup.PlayerBottomOsd.id to PlayerBottomOsdControl.entries.map {
                LayoutConfigItem(it.id, it.label)
            },
            LayoutConfigGroup.LivePlayerBottomOsd.id to LiveBottomOsdControl.entries.map {
                LayoutConfigItem(it.id, it.label)
            }
        )
    }

    fun applyLeftNav(items: List<LeftNaviItem> = LeftNaviItem.entries): List<LeftNaviItem> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyEnumGroup(LayoutConfigGroup.LeftNav, items) { it.name }
    }

    fun applyHome(items: List<HomeTopNavItem> = HomeTopNavItem.entries): List<HomeTopNavItem> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyEnumGroup(LayoutConfigGroup.Home, items) { it.name }
    }

    fun applyPgc(items: List<PgcTopNavItem> = PgcTopNavItem.entries): List<PgcTopNavItem> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyEnumGroup(LayoutConfigGroup.Pgc, items) { it.name }
    }

    fun applyPersonal(items: List<PersonalTopNavItem> = PersonalTopNavItem.entries): List<PersonalTopNavItem> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyEnumGroup(LayoutConfigGroup.Personal, items) { it.name }
    }

    fun applyLive(items: List<LiveCategory>): List<LiveCategory> {
        if (!Prefs.enableLayoutWebConfig) return items
        val defaults = items.map { LayoutConfigItem(id = it.key, label = it.label) }
        val saved = read().groups[LayoutConfigGroup.Live.id].orEmpty()
        val order = mergeItems(defaults, saved)
            .filterNot { it.hidden }
            .map { it.id }
        return order.mapNotNull { id -> items.firstOrNull { it.key == id } }
            .ifEmpty { items.take(1) }
    }

    fun applyPlayerBottomOsd(
        state: LayoutConfigState = read(),
        items: List<PlayerBottomOsdControl> = PlayerBottomOsdControl.entries
    ): List<PlayerBottomOsdControl> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyPlayerBottomOsdState(state, items)
    }

    internal fun applyPlayerBottomOsdState(
        state: LayoutConfigState,
        items: List<PlayerBottomOsdControl> = PlayerBottomOsdControl.entries
    ): List<PlayerBottomOsdControl> {
        val saved = state.groups[LayoutConfigGroup.PlayerBottomOsd.id].orEmpty()
        if (saved.isEmpty()) return items
        val defaultItems = items.map { LayoutConfigItem(id = it.id, label = it.label) }
        val order = mergeItems(defaultItems, saved)
            .filterNot { it.hidden }
            .map { it.id }
        return order.mapNotNull { id -> items.firstOrNull { it.id == id } }
            .ifEmpty { items.take(1) }
    }

    fun applyLivePlayerBottomOsd(
        state: LayoutConfigState = read(),
        items: List<LiveBottomOsdControl> = LiveBottomOsdControl.entries
    ): List<LiveBottomOsdControl> {
        if (!Prefs.enableLayoutWebConfig) return items
        return applyLivePlayerBottomOsdState(state, items)
    }

    internal fun applyLivePlayerBottomOsdState(
        state: LayoutConfigState,
        items: List<LiveBottomOsdControl> = LiveBottomOsdControl.entries
    ): List<LiveBottomOsdControl> {
        val saved = state.groups[LayoutConfigGroup.LivePlayerBottomOsd.id].orEmpty()
        if (saved.isEmpty()) return items
        val defaultItems = items.map { LayoutConfigItem(id = it.id, label = it.label) }
        val order = mergeItems(defaultItems, saved)
            .filterNot { it.hidden }
            .map { it.id }
        return order.mapNotNull { id -> items.firstOrNull { it.id == id } }
            .ifEmpty { items.take(1) }
    }

    fun toEditableState(liveCategories: List<LiveCategory> = emptyList()): LayoutConfigState {
        val state = normalize(read())
        val groups = state.groups.toMutableMap()
        if (liveCategories.isNotEmpty()) {
            val defaults = liveCategories.map { LayoutConfigItem(id = it.key, label = it.label) }
            groups[LayoutConfigGroup.Live.id] = mergeItems(
                defaultItems = defaults,
                savedItems = state.groups[LayoutConfigGroup.Live.id].orEmpty()
            )
        }
        return LayoutConfigState(groups)
    }

    private fun defaultLiveItems(): List<LayoutConfigItem> {
        return listOf(
            LayoutConfigItem("following", "我的关注"),
            LayoutConfigItem("recommend", "推荐直播")
        )
    }

    private fun <T> applyEnumGroup(
        group: LayoutConfigGroup,
        items: List<T>,
        idOf: (T) -> String
    ): List<T> {
        val saved = read().groups[group.id].orEmpty()
        if (saved.isEmpty()) return items
        val defaultItems = items.map { LayoutConfigItem(id = idOf(it), label = idOf(it)) }
        val order = mergeItems(defaultItems, saved)
            .filterNot { it.hidden }
            .map { it.id }
        return order.mapNotNull { id -> items.firstOrNull { idOf(it) == id } }
            .ifEmpty { items.take(1) }
    }

    private fun mergeItems(
        defaultItems: List<LayoutConfigItem>,
        savedItems: List<LayoutConfigItem>
    ): List<LayoutConfigItem> {
        val defaultsById = defaultItems.associateBy { it.id }
        val used = mutableSetOf<String>()
        val ordered = buildList {
            savedItems.forEach { saved ->
                val default = defaultsById[saved.id] ?: return@forEach
                used += saved.id
                add(default.copy(hidden = saved.hidden))
            }
            defaultItems.forEach { default ->
                if (default.id !in used) add(default)
            }
        }
        return if (ordered.any { !it.hidden }) ordered else ordered.mapIndexed { index, item ->
            if (index == 0) item.copy(hidden = false) else item
        }
    }
}

private val HomeTopNavItem.layoutLabel: String
    get() = when (this) {
        HomeTopNavItem.Recommend -> "推荐"
        HomeTopNavItem.Popular -> "热门"
        HomeTopNavItem.Ranking -> "排行榜"
        HomeTopNavItem.Douga -> "动画"
        HomeTopNavItem.Game -> "游戏"
        HomeTopNavItem.Kichiku -> "鬼畜"
        HomeTopNavItem.Music -> "音乐"
        HomeTopNavItem.Dance -> "舞蹈"
        HomeTopNavItem.Cinephile -> "影视"
        HomeTopNavItem.Ent -> "娱乐"
        HomeTopNavItem.Knowledge -> "知识"
        HomeTopNavItem.Tech -> "科技"
        HomeTopNavItem.Information -> "资讯"
        HomeTopNavItem.Food -> "美食"
        HomeTopNavItem.Life -> "生活"
        HomeTopNavItem.Car -> "汽车"
        HomeTopNavItem.Fashion -> "时尚"
        HomeTopNavItem.Sports -> "运动"
        HomeTopNavItem.Animal -> "动物圈"
    }

private val PgcTopNavItem.layoutLabel: String
    get() = when (this) {
        PgcTopNavItem.Cinema -> "影视"
        PgcTopNavItem.Anime -> "番剧"
        PgcTopNavItem.GuoChuang -> "国创"
        PgcTopNavItem.Movie -> "电影"
        PgcTopNavItem.Documentary -> "纪录片"
        PgcTopNavItem.Tv -> "电视剧"
        PgcTopNavItem.Variety -> "综艺"
    }

private val PersonalTopNavItem.layoutLabel: String
    get() = when (this) {
        PersonalTopNavItem.ToView -> "稍后再看"
        PersonalTopNavItem.History -> "历史"
        PersonalTopNavItem.Favorite -> "收藏"
        PersonalTopNavItem.FollowingSeason -> "我追的番"
    }
