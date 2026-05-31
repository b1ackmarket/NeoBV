package dev.aaa1115910.bv.component

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.touchClick

@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    isLargePadding: Boolean,
    downFocusRequester: FocusRequester = FocusRequester.Default,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember(items) {
        mutableStateListOf<FocusRequester>().apply {
            addAll(items.map { FocusRequester() })
        }
    }

    var selectedNav by remember { mutableStateOf(items.first()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val verticalPadding by animateDpAsState(
        targetValue = if (isLargePadding) 12.dp else 6.dp,
        label = "top nav vertical padding"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, verticalPadding),
        horizontalArrangement = Arrangement.Center
    ) {
        TabRow(
            modifier = Modifier
                .focusRestorer(focusRequester),
            selectedTabIndex = selectedTabIndex,
            separator = { Spacer(modifier = Modifier.width(12.dp)) },
        ) {
            items.forEachIndexed { index, tab ->
                NavItemTab(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                        .focusRequester(itemFocusRequesters[index]),
                    topNavItem = tab,
                    downFocusRequester = downFocusRequester,
                    selected = index == selectedTabIndex,
                    onFocus = {
                        selectedNav = tab
                        selectedTabIndex = index
                        onSelectedChanged(tab)
                    },
                    onClick = {
                        val clickResult = resolveTopNavClick(items = items, clicked = tab)
                        selectedNav = clickResult.selectedItem
                        selectedTabIndex = clickResult.selectedIndex
                        runCatching {
                            itemFocusRequesters[clickResult.selectedIndex].requestFocus()
                        }
                        onSelectedChanged(clickResult.selectedItem)
                        onClick(tab)
                    }
                )
            }
        }
    }
}

internal data class TopNavClickResult<T : TopNavItem>(
    val selectedItem: T,
    val selectedIndex: Int
)

internal fun <T : TopNavItem> resolveTopNavClick(
    items: List<T>,
    clicked: T
): TopNavClickResult<T> {
    val index = items.indexOf(clicked).takeIf { it >= 0 } ?: 0
    return TopNavClickResult(
        selectedItem = items.getOrElse(index) { clicked },
        selectedIndex = index
    )
}

@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    downFocusRequester: FocusRequester,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current

    Tab(
        modifier = modifier
            .touchClick(onClick)
            .focusProperties {
                down = downFocusRequester
            },
        selected = selected,
        onFocus = onFocus,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = topNavItem.getDisplayName(context),
            color = LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

interface TopNavItem {
    fun getDisplayName(context: Context = BVApp.context): String
}

enum class HomePageSettingItem(val code: Int, private val displayName: String) : TopNavItem {
    Recommend(1, "推荐"),
    Popular(2, "热门");

    companion object {
        fun fromCode(code: Int): HomePageSettingItem {
            return entries.find { it.code == code } ?: Recommend
        }
    }

    override fun getDisplayName(context: Context): String = displayName
}

enum class HomeTopNavItem(val code: Int, private val displayName: String) : TopNavItem {
    Recommend(0, "推荐"),
    Popular(1, "热门"),
    Ranking(2, "排行榜"),
    Douga(3, "动画"),
    Game(4, "游戏"),
    Kichiku(5, "鬼畜"),
    Music(6, "音乐"),
    Dance(7, "舞蹈"),
    Cinephile(8, "影视"),
    Ent(9, "娱乐"),
    Knowledge(10, "知识"),
    Tech(11, "科技"),
    Information(12, "资讯"),
    Food(13, "美食"),
    Life(14, "生活"),
    Car(15, "汽车"),
    Fashion(16, "时尚"),
    Sports(17, "运动"),
    Animal(18, "动物圈");

    companion object{
        fun fromCode(code: Int): HomeTopNavItem {
            return HomeTopNavItem.entries.find { it.code == code } ?: Recommend
        }
    }

    override fun getDisplayName(context: Context): String {
        return displayName
    }
}

enum class UgcTopNavItem(val ugcTypeV2: UgcTypeV2) : TopNavItem {
    Douga(UgcTypeV2.Douga),
    Game(UgcTypeV2.Game),
    Kichiku(UgcTypeV2.Kichiku),
    Music(UgcTypeV2.Music),
    Dance(UgcTypeV2.Dance),
    Cinephile(UgcTypeV2.Cinephile),
    Ent(UgcTypeV2.Ent),
    Knowledge(UgcTypeV2.Knowledge),
    Tech(UgcTypeV2.Tech),
    Information(UgcTypeV2.Information),
    Food(UgcTypeV2.Food),
    Life(UgcTypeV2.LifeJoy),
    Car(UgcTypeV2.Car),
    Fashion(UgcTypeV2.Fashion),
    Sports(UgcTypeV2.Sports),
    Animal(UgcTypeV2.Animal);

    override fun getDisplayName(context: Context): String {
        return ugcTypeV2.getDisplayName(context)
    }
}

enum class PgcTopNavItem(private val pgcType: PgcType) : TopNavItem {
    Anime(PgcType.Anime),
    GuoChuang(PgcType.GuoChuang),
    Movie(PgcType.Movie),
    Documentary(PgcType.Documentary),
    Tv(PgcType.Tv),
    Variety(PgcType.Variety);

    override fun getDisplayName(context: Context): String {
        return pgcType.getDisplayName(context)
    }
}

enum class PersonalTopNavItem : TopNavItem {
    ToView,
    History,
    Favorite,
    FollowingSeason;

    override fun getDisplayName(context: Context): String {
        return when (this) {
            ToView -> "稍后再看"
            History -> "历史"
            Favorite -> "收藏"
            FollowingSeason -> "我追的番"
        }
    }
}

enum class SearchTypeTopNavItem: TopNavItem {
    Video,
    MediaBangumi,
    MediaFt,
    Live,
    BiliUser;
    override fun getDisplayName(context: Context): String {
        return when (this) {
            Video -> "视频"
            MediaBangumi -> "番剧"
            MediaFt -> "影视"
            Live -> "直播"
            BiliUser -> "用户"
        }
    }
}
