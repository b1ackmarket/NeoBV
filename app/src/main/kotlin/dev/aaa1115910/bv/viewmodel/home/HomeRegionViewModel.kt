package dev.aaa1115910.bv.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.repositories.UgcRepository
import dev.aaa1115910.bv.component.HomeTopNavItem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HomeRegionViewModel(
    private val ugcRepository: UgcRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("HomeRegionViewModel")

    private val _regionStateMap = mutableMapOf<HomeTopNavItem, HomeRegionState>()
    val regionStateMap: Map<HomeTopNavItem, HomeRegionState> get() = _regionStateMap

    fun addState(item: HomeTopNavItem, state: HomeRegionState) {
        _regionStateMap[item] = state
        loadMore(item)
    }

    fun reloadAll(item: HomeTopNavItem) {
        val state = _regionStateMap[item] ?: return
        state.nextPage = dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedPage()
        state.hasMore = true
        state.items.clear()
        state.updating = false
        loadMore(item)
    }

    fun loadMore(item: HomeTopNavItem) {
        val state = _regionStateMap[item] ?: return
        if (state.updating || !state.hasMore) return

        viewModelScope.launch(Dispatchers.IO) {
            state.updating = true
            runCatching {
                val feedData = ugcRepository.getRegionFeedRcmd(state.ugcType, state.nextPage)
                state.items.addAll(feedData.items)
                state.nextPage = feedData.nextPage
                state.hasMore = feedData.items.isNotEmpty()
            }.onFailure {
                logger.warn(it) { "Load home region ${item.name} failed" }
            }
            state.updating = false
        }
    }
}
