package dev.aaa1115910.biliapi.entity.user

import dev.aaa1115910.biliapi.http.entity.user.AppSpaceVideoData
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceVideoTest {
    @Test
    fun `app space video preserves first cid when available`() {
        val item = AppSpaceVideoData.SpaceVideoItem(
            title = "demo",
            subtitle = "",
            tname = "",
            cover = "cover",
            uri = "",
            param = "123",
            goto = "av",
            length = "01:00",
            duration = 60,
            isPopular = false,
            isSteins = false,
            isUgcpay = false,
            isCooperation = false,
            isPgc = false,
            isLivePlayback = false,
            play = 1,
            danmaku = 2,
            ctime = 1,
            ugcPay = 0,
            author = "up",
            state = true,
            bvid = "BV1xx411c7mD",
            videos = 1,
            firstcid = 456L,
            cursorAttr = AppSpaceVideoData.SpaceVideoItem.CursorAttr(
                isLastWatchedArc = false,
                rank = 0
            ),
            iconType = 0
        )

        val video = SpaceVideo.fromSpaceVideoItem(item)

        assertEquals(456L, video.cid)
    }
}
