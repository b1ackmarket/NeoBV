package dev.aaa1115910.bv.viewmodel.player

import dev.aaa1115910.bv.component.HomePageSettingItem
import dev.aaa1115910.bv.screen.main.LeftNaviItem

enum class PlayerSidePanel {
    None,
    UpSpace,
    RelatedVideos
}

data class PlayerOverlayState(
    val activePanel: PlayerSidePanel = PlayerSidePanel.None,
    val pausePlaybackForPanel: Boolean = false
) {
    fun open(panel: PlayerSidePanel): PlayerOverlayState = copy(
        activePanel = panel,
        pausePlaybackForPanel = false
    )

    fun closePanel(): PlayerOverlayState = copy(
        activePanel = PlayerSidePanel.None,
        pausePlaybackForPanel = false
    )
}

enum class HomeStartDestination {
    Recommend,
    Popular
}

data class MainNavigationTarget(
    val leftNaviItem: LeftNaviItem,
    val homeStartDestination: HomeStartDestination?
)

object HomeEntryResolver {
    fun resolve(destination: HomeStartDestination): MainNavigationTarget {
        return when (destination) {
            HomeStartDestination.Recommend,
            HomeStartDestination.Popular -> MainNavigationTarget(
                leftNaviItem = LeftNaviItem.Home,
                homeStartDestination = destination
            )
        }
    }
}

fun HomePageSettingItem.toHomeStartDestination(): HomeStartDestination {
    return when (this) {
        HomePageSettingItem.Popular -> HomeStartDestination.Popular
        HomePageSettingItem.Recommend -> HomeStartDestination.Recommend
    }
}

data class TouchSupportPolicy(
    val requireLeanback: Boolean,
    val supportTouch: Boolean
) {
    companion object {
        fun forPhoneDebugging(): TouchSupportPolicy = TouchSupportPolicy(
            requireLeanback = false,
            supportTouch = true
        )
    }
}
