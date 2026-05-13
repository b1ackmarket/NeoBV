package dev.aaa1115910.bv.network

import kotlin.test.Test
import kotlin.test.assertEquals

class GithubApiTest {
    @Test
    fun `release repository points to NeoBV`() {
        assertEquals("b1ackmarket", GithubRepositoryConfig.OWNER)
        assertEquals("NeoBV", GithubRepositoryConfig.REPO)
    }

    @Test
    fun `release asset selector accepts current NeoBV workflow asset names`() {
        val assets = listOf(
            "mapping.zip",
            "NeoBV_229_0.3.4.release_default_universal.apk"
        )

        assertEquals(
            "NeoBV_229_0.3.4.release_default_universal.apk",
            selectUpdateApkAssetName(assets, isDebugBuild = false)
        )
    }

    @Test
    fun `release asset selector parses current NeoBV workflow version code`() {
        assertEquals(
            229,
            parseUpdateApkRevision("NeoBV_229_0.3.4.release_default_universal.apk")
        )
    }
}
