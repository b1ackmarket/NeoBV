package dev.aaa1115910.bv.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kuaishou.akdanmaku.data.DanmakuItemData
import dev.aaa1115910.bv.component.controllers.DanmakuType



/**
 * 对 PlayStateTips.kt 中的 toNetSpeedText() 私有扩展函数进行等价逻辑测试。
 * 由于该函数是私有的，这里直接复制逻辑进行验证。
 */
class NetSpeedFormatterTest {

    private fun Long.toNetSpeedText(): String {
        if (this <= 0L) return ""
        val kbps = this / 8.0 / 1024.0
        return if (kbps >= 1024) {
            " · ${String.format("%.1f", kbps / 1024)} MB/s"
        } else {
            " · ${String.format("%.1f", kbps)} KB/s"
        }
    }

    @Test
    fun `zero or negative speed returns empty string`() {
        assertEquals("", 0L.toNetSpeedText())
        assertEquals("", (-1000L).toNetSpeedText())
    }

    @Test
    fun `small speed displays in KB`() {
        // 8192 bits/s = 1 KB/s
        assertEquals(" · 1.0 KB/s", 8192L.toNetSpeedText())
        // 81920 bits/s = 10 KB/s
        assertEquals(" · 10.0 KB/s", 81920L.toNetSpeedText())
    }

    @Test
    fun `speed just below 1MB threshold stays in KB`() {
        // 1023 KB/s = 1023 * 8 * 1024 = 8,380,416 bits/s
        val bits = (1023.0 * 8 * 1024).toLong()
        val text = bits.toNetSpeedText()
        assertTrue(text.contains("KB/s"), "Expected KB unit, got: $text")
    }

    @Test
    fun `speed at exactly 1MB displays in MB`() {
        // 1024 KB/s = 1 MB/s = 1024 * 8 * 1024 = 8,388,608 bits/s
        val bits = (1024.0 * 8 * 1024).toLong()
        assertEquals(" · 1.0 MB/s", bits.toNetSpeedText())
    }

    @Test
    fun `large speed displays in MB`() {
        // 10 MB/s = 10 * 1024 * 8 * 1024 = 83,886,080 bits/s
        val bits = (10.0 * 1024 * 8 * 1024).toLong()
        assertEquals(" · 10.0 MB/s", bits.toNetSpeedText())
    }
}

/**
 * 对 VideoPlayerV3ViewModel 中的 applyDensity() 逻辑进行等价测试。
 */
class DanmakuDensityFilterTest {

    private fun generateFilters(
        enabledDanmakuTypes: List<DanmakuType>,
        density: Float
    ): List<com.kuaishou.akdanmaku.ecs.component.filter.DanmakuDataFilter> {
        val danmakuTypeFilter = TypeFilter()
        danmakuTypeFilter.clear()

        if (density <= 0f || enabledDanmakuTypes.isEmpty()) {
            danmakuTypeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_ROLLING)
            danmakuTypeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_TOP)
            danmakuTypeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM)
        } else {
            if (!enabledDanmakuTypes.contains(DanmakuType.All)) {
                val types = DanmakuType.entries.toMutableList()
                types.remove(DanmakuType.All)
                types.removeAll(enabledDanmakuTypes)
                val filterTypes = types.mapNotNull {
                    when (it) {
                        DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                        DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> null
                    }
                }
                filterTypes.forEach { danmakuTypeFilter.addFilterItem(it) }
            }
        }

        val filters = mutableListOf<com.kuaishou.akdanmaku.ecs.component.filter.DanmakuDataFilter>(danmakuTypeFilter)
        if (density > 0f && density < 1.0f && enabledDanmakuTypes.isNotEmpty()) {
            val maxCount = (30 * density).toInt().coerceAtLeast(1)
            filters.add(com.kuaishou.akdanmaku.ecs.component.filter.QuantityFilter(maxCount))
        }
        return filters
    }

    @Test
    fun `density 0f blocks all types`() {
        val filters = generateFilters(listOf(DanmakuType.All), 0f)
        assertEquals(1, filters.size)
        val typeFilter = filters[0] as TypeFilter
        val filterSet = typeFilter.filterSet
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_ROLLING))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_TOP))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM))
    }

    @Test
    fun `empty types blocks all types`() {
        val filters = generateFilters(emptyList(), 0.5f)
        assertEquals(1, filters.size)
        val typeFilter = filters[0] as TypeFilter
        val filterSet = typeFilter.filterSet
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_ROLLING))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_TOP))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM))
    }

    @Test
    fun `density 1_0 has no QuantityFilter`() {
        val filters = generateFilters(listOf(DanmakuType.All), 1.0f)
        assertEquals(1, filters.size)
        assertTrue(filters[0] is TypeFilter)
        assertTrue((filters[0] as TypeFilter).filterSet.isEmpty())
    }

    @Test
    fun `density 0_5 adds QuantityFilter`() {
        val filters = generateFilters(listOf(DanmakuType.All), 0.5f)
        assertEquals(2, filters.size)
        assertTrue(filters[0] is TypeFilter)
        assertTrue(filters[1] is com.kuaishou.akdanmaku.ecs.component.filter.QuantityFilter)
        
        val filter = filters[1]
        val getMaxCountMethod = filter.javaClass.getDeclaredMethod("getMaxCount")
        getMaxCountMethod.isAccessible = true
        val maxCount = getMaxCountMethod.invoke(filter) as Int
        assertEquals(15, maxCount)
    }

    @Test
    fun `partial types filters blocked modes`() {
        val filters = generateFilters(listOf(DanmakuType.Rolling), 1.0f)
        val typeFilter = filters[0] as TypeFilter
        val filterSet = typeFilter.filterSet
        assertFalse(filterSet.contains(DanmakuItemData.DANMAKU_MODE_ROLLING))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_TOP))
        assertTrue(filterSet.contains(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM))
    }
}

class DanmakuConfigReflectionTest {
    @Test
    fun printFieldsAndMethods() {
        val clazz = com.kuaishou.akdanmaku.DanmakuConfig::class.java
        println("=== Fields ===")
        clazz.declaredFields.forEach { println("${it.type.simpleName} ${it.name}") }
        println("=== Methods ===")
        clazz.declaredMethods.forEach { println("${it.returnType.simpleName} ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }})") }
    }
}









