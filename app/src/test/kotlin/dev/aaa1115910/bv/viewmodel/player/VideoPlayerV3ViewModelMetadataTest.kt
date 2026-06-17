package dev.aaa1115910.bv.viewmodel.player

import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.http.entity.reply.ReplyContent
import dev.aaa1115910.biliapi.http.entity.reply.ReplyEmote
import dev.aaa1115910.biliapi.http.entity.reply.ReplyItem
import dev.aaa1115910.biliapi.http.entity.reply.ReplyMember
import dev.aaa1115910.biliapi.http.entity.reply.ReplyPicture
import dev.aaa1115910.biliapi.http.entity.reply.ReplyControl
import dev.aaa1115910.biliapi.http.entity.reply.ReplyVip
import dev.aaa1115910.bv.entity.PlayerCommentSort
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.JumpModeQueueItem
import dev.aaa1115910.bv.subtitle.SecondarySubtitleOption
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig
import dev.aaa1115910.bv.ui.state.JumpModeState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SubtitleMemory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerV3ViewModelMetadataTest {
    @Test
    fun `same aid switch keeps publish date and play count text`() {
        val sb = java.lang.StringBuilder()
        sb.append("=== DanmakuFilter Reflection ===\n")
        try {
            val filterClass = Class.forName("com.kuaishou.akdanmaku.filter.DanmakuFilter")
            sb.append("Class methods:\n")
            filterClass.methods.forEach { m -> 
                val p = m.parameterTypes.joinToString { it.simpleName }
                sb.append("  ${m.returnType.simpleName} ${m.name}($p)\n") 
            }
            java.io.File("/Users/qinsher/Project/NeoBV-live-danmaku-blbl-rewrite/app/build/danmaku_filter_info.txt").writeText(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "P1",
            publishDateText = "5月8日",
            playCountText = "12.3万播放"
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 100L,
                cid = 20L,
                title = "P2"
            ),
            clearDetailMetadata = false
        )

        assertEquals("5月8日", nextState.publishDateText)
        assertEquals("12.3万播放", nextState.playCountText)
    }

    @Test
    fun `different aid switch clears publish date and play count text`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            publishDateText = "5月8日",
            playCountText = "12.3万播放"
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertEquals("", nextState.publishDateText)
        assertEquals("", nextState.playCountText)
    }

    @Test
    fun `video switch resets subtitle selection and loaded subtitle data`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            subtitleId = 42L,
            onlineCountText = "256",
            mediaStatsInfo = "video FPS: 30"
        ).copy(subtitleData = listOf())

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertEquals(-1L, nextState.subtitleId)
        assertTrue(nextState.subtitleData.isEmpty())
        assertTrue(nextState.subtitleList.isEmpty())
        assertEquals(null, nextState.onlineCountText)
        assertEquals("", nextState.mediaStatsInfo)
    }

    @Test
    fun `video switch preserves subtitle memory while clearing loaded subtitle data`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            subtitleId = 42L,
            subtitleList = listOf(subtitle(id = 42L, lang = "zh-CN", langDoc = "中文"))
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 100L,
                cid = 20L,
                title = "P2"
            ),
            clearDetailMetadata = false
        )

        assertEquals(42L, nextState.subtitleMemory?.id)
        assertEquals("zh-CN", nextState.subtitleMemory?.lang)
        assertEquals("中文", nextState.subtitleMemory?.langDoc)
        assertEquals(-1L, nextState.subtitleId)
        assertTrue(nextState.subtitleData.isEmpty())
        assertTrue(nextState.subtitleList.isEmpty())
    }

    @Test
    fun `video switch preserves current play speed`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            playSpeed = 1.5f
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 100L,
                cid = 20L,
                title = "P2"
            ),
            clearDetailMetadata = false
        )

        assertEquals(1.5f, nextState.playSpeed)
    }

    @Test
    fun `subtitle memory prefers same language over first available subtitle`() {
        val remembered = SubtitleMemory(id = 11L, lang = "zh-CN", langDoc = "中文")
        val tracks = listOf(
            subtitle(id = 21L, lang = "en", langDoc = "English"),
            subtitle(id = 22L, lang = "zh-CN", langDoc = "中文（自动生成）")
        )

        assertEquals(22L, resolveRememberedSubtitleId(remembered, tracks))
    }

    @Test
    fun `subtitle memory falls back to first subtitle when language is unavailable`() {
        val remembered = SubtitleMemory(id = 11L, lang = "zh-CN", langDoc = "中文")
        val tracks = listOf(
            subtitle(id = 21L, lang = "en", langDoc = "English"),
            subtitle(id = 22L, lang = "ja", langDoc = "日语")
        )

        assertEquals(21L, resolveRememberedSubtitleId(remembered, tracks))
    }

    @Test
    fun `next play target prefers ugc page before collection video`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "P1",
            availableVideoList = listOf(
                VideoListItem(
                    aid = 100L,
                    cid = 10L,
                    title = "合集视频一",
                    ugcPages = listOf(
                        VideoPage(cid = 10L, index = 1, title = "P1", duration = 60, dimension = Dimension(1920, 1080)),
                        VideoPage(cid = 20L, index = 2, title = "P2", duration = 60, dimension = Dimension(1920, 1080))
                    )
                ),
                VideoListItem(aid = 200L, cid = 30L, title = "合集视频二")
            )
        )

        val target = resolveAutoNextTarget(currentState)

        assertEquals(AutoNextTarget.NextVideo(aid = 100L, cid = 20L, title = "P2"), target)
    }

    @Test
    fun `auto next target falls back to first related video`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Last",
            availableVideoList = listOf(VideoListItem(aid = 100L, cid = 10L, title = "Last")),
            relatedVideos = listOf(
                VideoCardData(avid = 200L, cid = null, title = "无 cid 相关视频", cover = "", upName = ""),
                VideoCardData(avid = 300L, cid = 40L, title = "相关视频", cover = "", upName = "")
            )
        )

        val target = resolveAutoNextTarget(currentState)

        assertEquals(AutoNextTarget.RelatedVideo(aid = 300L, cid = 40L, title = "相关视频", cover = ""), target)
    }

    @Test
    fun `jump mode switch keeps queue and enabled state`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            jumpModeState = JumpModeState(
                available = true,
                enabled = true,
                currentIndex = 0,
                items = listOf(
                    JumpModeQueueItem(aid = 100L, title = "Old"),
                    JumpModeQueueItem(aid = 200L, title = "New")
                )
            )
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertTrue(nextState.jumpModeState.enabled)
        assertEquals(1, nextState.jumpModeState.currentIndex)
    }

    @Test
    fun `jump mode switch disables when new video is outside queue`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            jumpModeState = JumpModeState(
                available = true,
                enabled = true,
                currentIndex = 0,
                items = listOf(
                    JumpModeQueueItem(aid = 100L, title = "Old"),
                    JumpModeQueueItem(aid = 200L, title = "Queued")
                )
            )
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 300L,
                cid = 30L,
                title = "Outside"
            ),
            clearDetailMetadata = true
        )

        assertFalse(nextState.jumpModeState.available)
        assertFalse(nextState.jumpModeState.enabled)
    }

    @Test
    fun `bili media stats hides empty fields and keeps selected stream facts`() {
        val stats = buildBiliMediaStatsInfo(
            video = DashVideo(
                quality = 80,
                baseUrl = "https://video",
                bandwidth = 2_500_000,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl = emptyList(),
                codecs = "avc1.640032"
            ),
            audio = DashAudio(
                baseUrl = "https://audio",
                bandwidth = 192_000,
                codecId = 30280,
                backUrl = emptyList(),
                codecs = "mp4a.40.2"
            )
        )

        assertEquals(
            """
            resolution: 1920 x 1080
            video FPS: 30
            stream bitrate: 2.50 Mbps
            video codec: avc1.640032
            audio codec: mp4a.40.2
            audio bitrate: 192 kbps
            """.trimIndent(),
            stats
        )
    }

    @Test
    fun `bili media stats does not render unknown placeholders`() {
        val stats = buildBiliMediaStatsInfo(
            video = DashVideo(
                quality = 80,
                baseUrl = "https://video",
                bandwidth = 0,
                codecId = 0,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = ""
            ),
            audio = null
        )

        assertEquals("", stats)
    }

    @Test
    fun `reply sort maps to bilibili api sort values`() {
        assertEquals(0, PlayerCommentSort.Latest.toReplyApiSort())
        assertEquals(1, PlayerCommentSort.Hot.toReplyApiSort())
    }

    @Test
    fun `preferred main subtitle falls back to first available track`() {
        val tracks = listOf(
            subtitle(id = 2L, lang = "en", langDoc = "英语"),
            subtitle(id = 1L, lang = "zh", langDoc = "中文")
        )

        assertEquals(2L, resolvePreferredMainSubtitleId(null, tracks))
    }

    @Test
    fun `preferred secondary subtitle stays off without memory`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
            .let { it.copy(verifiedSignature = it.configSignature()) }
        val tracks = listOf(
            subtitle(id = 1L, lang = "zh", langDoc = "中文"),
            subtitle(id = 2L, lang = "en", langDoc = "英语")
        )

        val option = resolvePreferredSecondarySubtitleOption(
            tracks = tracks,
            mainSubtitleId = 1L,
            memory = null,
            config = config,
            preferCustom = true,
            sourceSubtitleAvailable = true
        )

        assertEquals(null, option)
    }

    @Test
    fun `osd secondary subtitle can choose custom translation`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
            .let { it.copy(verifiedSignature = it.configSignature()) }
        val tracks = listOf(
            subtitle(id = 1L, lang = "zh", langDoc = "中文"),
            subtitle(id = 2L, lang = "en", langDoc = "英语")
        )

        val option = resolveOsdSecondarySubtitleOption(
            tracks = tracks,
            mainSubtitleId = 1L,
            memory = null,
            config = config,
            preferCustom = true,
            sourceSubtitleAvailable = true
        )

        assertEquals(SecondarySubtitleOption.CustomTranslation, option)
    }

    @Test
    fun `reply item maps to player comment item`() {
        val item = ReplyItem(
            rpid = 123L,
            mid = 456L,
            ctime = 1_700_000_000L,
            like = 32,
            rcount = 7,
            member = ReplyMember(
                uname = "评论用户",
                avatar = "https://avatar",
                vip = ReplyVip(nicknameColor = "#FB7299")
            ),
            content = ReplyContent(
                message = "这是一条评论",
                pictures = listOf(ReplyPicture(imgSrc = "https://pic", imgWidth = 640, imgHeight = 360)),
                emote = mapOf(
                    "[大笑]" to ReplyEmote(text = "[大笑]", url = "https://emoji/daxiao.png", size = 1),
                    "[妙啊]" to ReplyEmote(text = "[妙啊]", url = "https://emoji/miao.png", size = 2)
                )
            ),
            replyControl = ReplyControl(location = "IP属地：上海")
        )

        val comment = item.toPlayerCommentItem()

        assertEquals("123", comment.id)
        assertEquals(456L, comment.mid)
        assertEquals("评论用户", comment.username)
        assertEquals("https://avatar", comment.avatar)
        assertEquals("这是一条评论", comment.message)
        assertEquals("[大笑]", comment.emotes[0].text)
        assertEquals("https://emoji/daxiao.png", comment.emotes[0].url)
        assertEquals("[妙啊]", comment.emotes[1].text)
        assertEquals(2, comment.emotes[1].size)
        assertEquals("https://pic", comment.pictures.single().url)
        assertEquals("32赞", comment.likeText)
        assertEquals("7回复", comment.replyText)
        assertEquals("IP属地：上海", comment.ipLocation)
        assertEquals(0xFB7299, comment.color)
    }

    @Test
    fun `comment total count formats for panel title`() {
        assertEquals("", formatCommentTotalCount(null))
        assertEquals("", formatCommentTotalCount(0))
        assertEquals("999条", formatCommentTotalCount(999))
        assertEquals("1.2万条", formatCommentTotalCount(12_000))
    }

    private fun subtitle(
        id: Long,
        lang: String,
        langDoc: String
    ) = Subtitle(
        id = id,
        lang = lang,
        langDoc = langDoc,
        url = "https://example.com/$id.json",
        type = SubtitleType.CC,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None
    )

}
