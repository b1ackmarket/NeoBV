package dev.aaa1115910.bv.cast

import dev.aaa1115910.bv.cast.protocol.CastClientHint
import dev.aaa1115910.bv.cast.protocol.CastContent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PiliPlusUgcCastResolverTest {
    @Test
    fun `resolves unique exact title candidate with matching cid`() = runBlocking {
        val resolver = resolver(
            candidates = listOf(
                PiliPlusUgcSearchCandidate(
                    aid = 70156862L,
                    title = "<em class=\"keyword\">王道计算机考研 操作系统</em>"
                )
            ),
            details = mapOf(
                70156862L to PiliPlusUgcVideoDetail(
                    aid = 70156862L,
                    title = "王道计算机考研 操作系统",
                    pages = listOf(PiliPlusUgcVideoPage(cid = 1103498484L, title = "15. 处理器运行模式"))
                )
            )
        )

        val resolved = resolver.resolve(
            CastContent(
                title = "王道计算机考研 操作系统",
                directMediaUrl = "https://upos-sz-mirrorcoso1.bilivideo.com/upgcxcode/84/98/1103498484/1103498484-1-30080.m4s?deadline=1",
                clientHint = CastClientHint.PiliPlus
            )
        )

        assertEquals(
            PiliPlusResolvedVideo(
                aid = 70156862L,
                cid = 1103498484L,
                title = "王道计算机考研 操作系统",
                partTitle = "15. 处理器运行模式"
            ),
            resolved
        )
    }

    @Test
    fun `does not resolve without cid`() = runBlocking {
        val resolver = resolver(
            candidates = listOf(PiliPlusUgcSearchCandidate(1L, "同名标题")),
            details = mapOf(1L to detail(1L, "同名标题", 10L))
        )

        val resolved = resolver.resolve(
            CastContent(
                title = "同名标题",
                directMediaUrl = "https://upos-sz-mirrorcoso1.bilivideo.com/video.m4s?deadline=1",
                clientHint = CastClientHint.PiliPlus
            )
        )

        assertNull(resolved)
    }

    @Test
    fun `does not resolve fuzzy title matches`() = runBlocking {
        val resolver = resolver(
            candidates = listOf(PiliPlusUgcSearchCandidate(1L, "同名标题 解说")),
            details = mapOf(1L to detail(1L, "同名标题 解说", 1103498484L))
        )

        val resolved = resolver.resolve(
            CastContent(
                title = "同名标题",
                directMediaUrl = "https://upos-sz-mirrorcoso1.bilivideo.com/1103498484.m4s",
                clientHint = CastClientHint.PiliPlus
            )
        )

        assertNull(resolved)
    }

    @Test
    fun `does not resolve when multiple exact candidates contain cid`() = runBlocking {
        val resolver = resolver(
            candidates = listOf(
                PiliPlusUgcSearchCandidate(1L, "同名标题"),
                PiliPlusUgcSearchCandidate(2L, "同名标题")
            ),
            details = mapOf(
                1L to detail(1L, "同名标题", 1103498484L),
                2L to detail(2L, "同名标题", 1103498484L)
            )
        )

        val resolved = resolver.resolve(
            CastContent(
                title = "同名标题",
                directMediaUrl = "https://upos-sz-mirrorcoso1.bilivideo.com/1103498484.m4s",
                clientHint = CastClientHint.PiliPlus
            )
        )

        assertNull(resolved)
    }

    private fun resolver(
        candidates: List<PiliPlusUgcSearchCandidate>,
        details: Map<Long, PiliPlusUgcVideoDetail>
    ) = PiliPlusUgcCastResolver(
        searchVideos = { candidates },
        loadVideoDetail = { aid -> details[aid] }
    )

    private fun detail(aid: Long, title: String, cid: Long) =
        PiliPlusUgcVideoDetail(
            aid = aid,
            title = title,
            pages = listOf(PiliPlusUgcVideoPage(cid = cid, title = "正片"))
        )
}
