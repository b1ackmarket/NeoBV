package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateListOf
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedPage

data class HomeRegionState(
    val lazyGridState: LazyGridState,
    val ugcType: UgcTypeV2,
    val items: MutableList<UgcItem> = mutableStateListOf(),
    var nextPage: UgcFeedPage = UgcFeedPage(),
    var hasMore: Boolean = true,
    var updating: Boolean = false
)
