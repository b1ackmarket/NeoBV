package dev.aaa1115910.bv.plugin.impl.sponsorblock

import dev.aaa1115910.bv.plugin.api.PlayerPluginContext
import dev.aaa1115910.bv.plugin.api.PluginPlaybackAction
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsorBlockPluginTest {
    @Test
    fun `disabled plugin ignores playback callbacks`() = runBlocking {
        val plugin = SponsorBlockPlugin(
            api = FakeSponsorBlockApi(emptyList()),
            configStore = InMemorySponsorBlockConfigStore(enabled = false)
        )

        plugin.onVideoLoaded(
            PlayerPluginContext(
                aid = 1L,
                cid = 2L,
                bvid = "BV1xx411c7mD",
                title = "test"
            )
        )

        val action = plugin.onPlaybackPosition(12_000L)

        assertTrue(action is PluginPlaybackAction.None)
        assertEquals(0, plugin.debugLoadedSegmentCount())
    }

    @Test
    fun `default config prompts for sponsor and intro segments`() = runBlocking {
        val plugin = SponsorBlockPlugin(
            api = FakeSponsorBlockApi(
                listOf(
                    SponsorSegment("seg-sponsor", "sponsor", 10_000L, 20_000L),
                    SponsorSegment("seg-intro", "intro", 30_000L, 45_000L)
                )
            ),
            configStore = InMemorySponsorBlockConfigStore(
                enabled = true,
                config = SponsorBlockConfig.default()
            )
        )

        plugin.onVideoLoaded(
            PlayerPluginContext(
                aid = 1L,
                cid = 2L,
                bvid = "BV1xx411c7mD",
                title = "test"
            )
        )

        val sponsorAction = plugin.onPlaybackPosition(12_000L)
        val introAction = plugin.onPlaybackPosition(31_000L)

        assertEquals(
            PluginPlaybackAction.PromptSkip("seg-sponsor", 10_000L, 20_000L, "显示提示：赞助/恰饭", "sponsor"),
            sponsorAction
        )
        assertEquals(
            PluginPlaybackAction.PromptSkip("seg-intro", 30_000L, 45_000L, "显示提示：片头", "intro"),
            introAction
        )
    }

    @Test
    fun `default config prompts for sponsor categories and disables filler categories`() {
        val config = SponsorBlockConfig.default()

        assertEquals(SkipPolicy.Prompt, config.categoryPolicy["sponsor"])
        assertEquals(SkipPolicy.Prompt, config.categoryPolicy["selfpromo"])
        assertEquals(SkipPolicy.Prompt, config.categoryPolicy["intro"])
        assertEquals(SkipPolicy.Prompt, config.categoryPolicy["outro"])
        assertEquals(SkipPolicy.Disabled, config.categoryPolicy["interaction"])
        assertEquals(SkipPolicy.Disabled, config.categoryPolicy["poi_highlight"])
        assertEquals(SkipPolicy.Disabled, config.categoryPolicy["preview"])
        assertEquals(SkipPolicy.Disabled, config.categoryPolicy["filler"])
        assertEquals(SkipPolicy.Disabled, config.categoryPolicy["music_offtopic"])
    }

    @Test
    fun `dismissed prompt stays suppressed until leaving segment and can prompt again after re-enter`() =
        runBlocking {
            val plugin = SponsorBlockPlugin(
                api = FakeSponsorBlockApi(
                    listOf(SponsorSegment("seg-sponsor", "sponsor", 10_000L, 20_000L))
                ),
                configStore = InMemorySponsorBlockConfigStore(
                    enabled = true,
                    config = SponsorBlockConfig.default()
                )
            )

            plugin.onVideoLoaded(
                PlayerPluginContext(
                    aid = 1L,
                    cid = 2L,
                    bvid = "BV1xx411c7mD",
                    title = "test"
                )
            )

            val firstPrompt = plugin.onPlaybackPosition(12_000L)
            plugin.dismissPrompt("seg-sponsor")
            val suppressedWhileInside = plugin.onPlaybackPosition(13_000L)
            val afterLeaving = plugin.onPlaybackPosition(21_000L)
            val promptAfterReenter = plugin.onPlaybackPosition(12_500L)

            assertEquals(
                PluginPlaybackAction.PromptSkip(
                    "seg-sponsor",
                    10_000L,
                    20_000L,
                    "显示提示：赞助/恰饭",
                    "sponsor"
                ),
                firstPrompt
            )
            assertTrue(suppressedWhileInside is PluginPlaybackAction.None)
            assertTrue(afterLeaving is PluginPlaybackAction.None)
            assertEquals(
                PluginPlaybackAction.PromptSkip(
                    "seg-sponsor",
                    10_000L,
                    20_000L,
                    "显示提示：赞助/恰饭",
                    "sponsor"
                ),
                promptAfterReenter
            )
        }

    @Test
    fun `confirming a prompt prevents re-prompting when re-entering the segment`() = runBlocking {
        val plugin = SponsorBlockPlugin(
            api = FakeSponsorBlockApi(
                listOf(SponsorSegment("seg-sponsor", "sponsor", 10_000L, 20_000L))
            ),
            configStore = InMemorySponsorBlockConfigStore(
                enabled = true,
                config = SponsorBlockConfig.default()
            )
        )

        plugin.onVideoLoaded(
            PlayerPluginContext(
                aid = 1L,
                cid = 2L,
                bvid = "BV1xx411c7mD",
                title = "test"
            )
        )

        val firstPrompt = plugin.onPlaybackPosition(12_000L)
        plugin.markHandled("seg-sponsor")
        val afterConfirm = plugin.onPlaybackPosition(21_000L)
        val reenterAfterConfirm = plugin.onPlaybackPosition(12_500L)

        assertEquals(
            PluginPlaybackAction.PromptSkip(
                "seg-sponsor",
                10_000L,
                20_000L,
                "显示提示：赞助/恰饭",
                "sponsor"
            ),
            firstPrompt
        )
        assertTrue(afterConfirm is PluginPlaybackAction.None)
        assertTrue(reenterAfterConfirm is PluginPlaybackAction.None)
    }

    @Test
    fun `store enabled state is authoritative over stale config enabled flag`() = runBlocking {
        val plugin = SponsorBlockPlugin(
            api = FakeSponsorBlockApi(
                listOf(SponsorSegment("seg-sponsor", "sponsor", 10_000L, 20_000L))
            ),
            configStore = InMemorySponsorBlockConfigStore(
                enabled = true,
                config = SponsorBlockConfig.default().copy(enabled = false)
            )
        )

        plugin.onVideoLoaded(
            PlayerPluginContext(
                aid = 1L,
                cid = 2L,
                bvid = "BV1xx411c7mD",
                title = "test"
            )
        )

        val action = plugin.onPlaybackPosition(12_000L)

        assertEquals(
            PluginPlaybackAction.PromptSkip(
                "seg-sponsor",
                10_000L,
                20_000L,
                "显示提示：赞助/恰饭",
                "sponsor"
            ),
            action
        )
    }

    @Test
    fun `plugin ignores segments before five seconds but can still trigger afterwards`() = runBlocking {
        val plugin = SponsorBlockPlugin(
            api = FakeSponsorBlockApi(
                listOf(SponsorSegment("seg-intro", "intro", 0L, 8_000L))
            ),
            configStore = InMemorySponsorBlockConfigStore(
                enabled = true,
                config = SponsorBlockConfig.default()
            )
        )

        plugin.onVideoLoaded(
            PlayerPluginContext(
                aid = 1L,
                cid = 2L,
                bvid = "BV1xx411c7mD",
                title = "test"
            )
        )

        assertTrue(plugin.onPlaybackPosition(4_000L) is PluginPlaybackAction.None)
        assertEquals(
            PluginPlaybackAction.PromptSkip(
                "seg-intro",
                0L,
                8_000L,
                "显示提示：片头",
                "intro"
            ),
            plugin.onPlaybackPosition(5_000L)
        )
    }

    private class FakeSponsorBlockApi(
        private val segments: List<SponsorSegment>
    ) : SponsorBlockApi {
        override suspend fun getSegments(bvid: String): List<SponsorSegment> = segments
    }

    private class InMemorySponsorBlockConfigStore(
        enabled: Boolean,
        config: SponsorBlockConfig = SponsorBlockConfig.default()
    ) : SponsorBlockConfigStore {
        private var currentConfig = config
        private var currentEnabled = enabled

        override suspend fun readConfig(): SponsorBlockConfig = currentConfig

        override suspend fun writeConfig(config: SponsorBlockConfig) {
            currentConfig = config
        }

        override suspend fun isEnabled(): Boolean = currentEnabled

        override suspend fun setEnabled(enabled: Boolean) {
            currentEnabled = enabled
        }
    }
}
