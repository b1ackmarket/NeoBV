package dev.aaa1115910.bv.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutConfigTest {
    @Test
    fun `default groups expose player bottom osd controls`() {
        val playerItems = LayoutConfig.defaultGroups().getValue(LayoutConfigGroup.PlayerBottomOsd.id)

        assertEquals(PlayerBottomOsdControl.entries.map { it.id }, playerItems.map { it.id })
        assertTrue(playerItems.any { it.label == "播放设置" })
    }

    @Test
    fun `default groups do not expose legacy ugc top tabs`() {
        assertFalse(LayoutConfig.defaultGroups().containsKey("ugc"))
    }

    @Test
    fun `player bottom osd controls can be reordered and hidden`() {
        val state = LayoutConfigState(
            groups = mapOf(
                LayoutConfigGroup.PlayerBottomOsd.id to listOf(
                    LayoutConfigItem(PlayerBottomOsdControl.Settings.id, "播放设置"),
                    LayoutConfigItem(PlayerBottomOsdControl.Comments.id, "评论", hidden = true),
                    LayoutConfigItem(PlayerBottomOsdControl.VideoList.id, "选集")
                )
            )
        )

        val controls = LayoutConfig.applyPlayerBottomOsdState(
            state = LayoutConfig.normalize(state),
            items = listOf(
                PlayerBottomOsdControl.VideoList,
                PlayerBottomOsdControl.Comments,
                PlayerBottomOsdControl.Settings
            )
        )

        assertEquals(
            listOf(PlayerBottomOsdControl.Settings, PlayerBottomOsdControl.VideoList),
            controls
        )
    }

    @Test
    fun `normalization keeps at least one visible item per group`() {
        val state = LayoutConfigState(
            groups = mapOf(
                LayoutConfigGroup.PlayerBottomOsd.id to PlayerBottomOsdControl.entries.map {
                    LayoutConfigItem(it.id, it.label, hidden = true)
                }
            )
        )

        val normalized = LayoutConfig.normalize(state)
        val playerItems = normalized.groups.getValue(LayoutConfigGroup.PlayerBottomOsd.id)

        assertFalse(playerItems.first().hidden)
        assertTrue(playerItems.drop(1).all { it.hidden })
    }
}
