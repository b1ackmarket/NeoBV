package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.bv.entity.live.LiveCategory
import dev.aaa1115910.bv.entity.live.LiveRoomCard
import dev.aaa1115910.bv.repository.LiveRepository
import dev.aaa1115910.bv.util.swapListWithMainContext
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
    val categories = mutableStateListOf<LiveCategory>()
    val rooms = mutableStateListOf<LiveRoomCard>()

    val isLogin get() = bvUserRepository.isLogin
    var selectedCategoryIndex by mutableStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set

    init {
        if (isLogin) {
            ensureLoaded()
        }
    }

    fun ensureLoaded() {
        if (!isLogin || categories.isNotEmpty() || loading) return
        viewModelScope.launch(Dispatchers.IO) {
            loadCategories()
        }
    }

    fun onLoginStateChanged(isLogin: Boolean) {
        if (!isLogin) {
            clearData()
            return
        }
        ensureLoaded()
    }

    fun selectCategory(index: Int) {
        if (index == selectedCategoryIndex) return
        selectedCategoryIndex = index
        loadCategory(index)
    }

    fun refresh() {
        if (!isLogin) return
        if (categories.isEmpty()) {
            ensureLoaded()
            return
        }
        loadCategory(selectedCategoryIndex)
    }

    private suspend fun loadCategories() {
        withContext(Dispatchers.Main) {
            loading = true
        }
        try {
            val loaded = liveRepository.getCategories()
            categories.swapListWithMainContext(loaded)
            selectedCategoryIndex = 0
            if (loaded.isNotEmpty()) {
                loadCategory(0)
            } else {
                rooms.swapListWithMainContext(emptyList())
            }
        } finally {
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    private fun loadCategory(index: Int) {
        if (!isLogin) return
        val category = categories.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loading = true
            }
            try {
                val loadedRooms = liveRepository.getRooms(category)
                rooms.swapListWithMainContext(loadedRooms)
            } finally {
                withContext(Dispatchers.Main) {
                    loading = false
                }
            }
        }
    }

    private fun clearData() {
        categories.clear()
        rooms.clear()
        selectedCategoryIndex = 0
        loading = false
    }
}
