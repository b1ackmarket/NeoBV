package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.bv.entity.live.LiveCategory
import dev.aaa1115910.bv.entity.live.LiveRoomCard
import dev.aaa1115910.bv.entity.live.LiveCategoryType
import dev.aaa1115910.bv.repository.LiveRepository
import dev.aaa1115910.bv.util.swapList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository,
    private val bvUserRepository: BvUserRepository
) : ViewModel() {
    private companion object {
        const val PageSize = 30
    }

    val categories = mutableStateListOf<LiveCategory>()
    val rooms = mutableStateListOf<LiveRoomCard>()

    val isLogin get() = bvUserRepository.isLogin
    var selectedCategoryIndex by mutableStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set
    private var nextPage = 1
    private var canLoadMore = true

    init {
        ensureLoaded()
    }

    fun ensureLoaded() {
        if (categories.isNotEmpty() || loading) return
        viewModelScope.launch(Dispatchers.IO) {
            loadCategories()
        }
    }

    fun onLoginStateChanged(isLogin: Boolean) {
        if (categories.isEmpty()) {
            ensureLoaded()
            return
        }
        val selectedCategory = categories.getOrNull(selectedCategoryIndex)
        if (isLogin && selectedCategory?.type == LiveCategoryType.Following && rooms.isEmpty()) {
            loadCategory(selectedCategoryIndex, append = false)
        } else if (!isLogin && selectedCategory?.type == LiveCategoryType.Following) {
            rooms.clear()
            nextPage = 1
            canLoadMore = false
        }
    }

    fun selectCategory(index: Int) {
        if (index == selectedCategoryIndex) return
        selectedCategoryIndex = index
        loadCategory(index, append = false)
    }

    fun refresh() {
        if (categories.isEmpty()) {
            ensureLoaded()
            return
        }
        loadCategory(selectedCategoryIndex, append = false)
    }

    fun loadMoreIfNeeded(focusedIndex: Int) {
        if (loading || !canLoadMore) return
        if (!dev.aaa1115910.bv.screen.main.live.shouldLoadMoreLiveRooms(
                focusedIndex = focusedIndex,
                roomCount = rooms.size
            )
        ) {
            return
        }
        loadCategory(selectedCategoryIndex, append = true)
    }

    private suspend fun loadCategories() {
        withContext(Dispatchers.Main) {
            loading = true
        }
        try {
            val loaded = liveRepository.getCategories()
            if (loaded.isNotEmpty()) {
                val initialIndex = initialCategoryIndex(loaded, isLogin)
                val loadedRooms = liveRepository.getRooms(
                    category = loaded[initialIndex],
                    page = 1,
                    pageSize = PageSize
                )
                withContext(Dispatchers.Main) {
                    categories.swapList(loaded)
                    selectedCategoryIndex = initialIndex
                    rooms.swapList(loadedRooms)
                    nextPage = 2
                    canLoadMore = loadedRooms.size >= PageSize
                }
            } else {
                withContext(Dispatchers.Main) {
                    categories.swapList(loaded)
                    selectedCategoryIndex = initialCategoryIndex(loaded, isLogin)
                    rooms.swapList(emptyList())
                    nextPage = 1
                    canLoadMore = false
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    private fun loadCategory(index: Int, append: Boolean) {
        val category = categories.getOrNull(index) ?: return
        if (!isLogin && category.type == LiveCategoryType.Following) {
            rooms.clear()
            nextPage = 1
            canLoadMore = false
            loading = false
            return
        }
        val requestedPage = if (append) nextPage else 1
        loading = true
        if (!append) {
            rooms.clear()
            nextPage = 1
            canLoadMore = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedRooms = liveRepository.getRooms(
                    category = category,
                    page = requestedPage,
                    pageSize = PageSize
                )
                withContext(Dispatchers.Main) {
                    if (categories.getOrNull(selectedCategoryIndex)?.key != category.key) {
                        return@withContext
                    }
                    if (append) {
                        val existingRoomIds = rooms.map { it.roomId }.toSet()
                        rooms.addAll(loadedRooms.filterNot { it.roomId in existingRoomIds })
                    } else {
                        rooms.clear()
                        rooms.addAll(loadedRooms)
                    }
                    nextPage = requestedPage + 1
                    canLoadMore = loadedRooms.size >= PageSize
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loading = false
                }
            }
        }
    }

    private fun initialCategoryIndex(loaded: List<LiveCategory>, isLogin: Boolean): Int {
        if (isLogin) return 0
        return loaded.indexOfFirst { it.type == LiveCategoryType.Recommend }
            .takeIf { it >= 0 }
            ?: 0
    }
}
