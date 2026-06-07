package dev.aaa1115910.bv.screen

import dev.aaa1115910.biliapi.entity.user.Author
import dev.aaa1115910.biliapi.entity.video.VideoStaff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoInfoScreenPolicyTest {
    @Test
    fun `video detail follow button is hidden for multiple coauthors`() {
        assertTrue(shouldShowVideoDetailFollowButton(staffCount = 0))
        assertFalse(shouldShowVideoDetailFollowButton(staffCount = 1))
        assertFalse(shouldShowVideoDetailFollowButton(staffCount = 2))
    }

    @Test
    fun `coauthor chips include owner and dedupe staff`() {
        val authors = buildVideoDetailAuthorChips(
            owner = Author(mid = 1, name = "小潮院长", face = "owner-face"),
            staff = listOf(
                VideoStaff(mid = 1, name = "小潮院长", face = "staff-face", title = "UP主", follower = 0),
                VideoStaff(mid = 2, name = "杜海皇", face = "face-2", title = "联合创作", follower = 0)
            )
        )

        assertEquals(listOf(1L, 2L), authors.map { it.mid })
        assertEquals("owner-face", authors.first().face)
    }
}
