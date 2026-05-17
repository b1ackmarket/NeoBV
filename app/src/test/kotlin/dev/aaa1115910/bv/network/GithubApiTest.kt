package dev.aaa1115910.bv.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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
            "NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk"
        )

        assertEquals(
            "NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk",
            selectUpdateApkAssetName(assets, isDebugBuild = false)
        )
    }

    @Test
    fun `release asset selector parses current NeoBV workflow version code`() {
        assertEquals(
            934,
            parseUpdateApkRevision("NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk")
        )
    }

    @Test
    fun `release asset selector rejects legacy loose names`() {
        val assets = listOf(
            "NeoBV_229_0.3.4.release_default_universal.apk",
            "NeoBV_934_0.4.0.r934.0618869d.alpha_default_universal.apk"
        )

        assertEquals(
            null,
            selectUpdateApkAssetName(
                assetNames = assets,
                isDebugBuild = false,
                type = UpdateReleaseType.Release
            )
        )
    }

    @Test
    fun `alpha asset selector only accepts alpha assets`() {
        val assets = listOf(
            "NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk",
            "NeoBV_935_0.4.0.r935.7b8020ee.alpha_default_universal.apk"
        )

        assertEquals(
            "NeoBV_935_0.4.0.r935.7b8020ee.alpha_default_universal.apk",
            selectUpdateApkAssetName(
                assetNames = assets,
                isDebugBuild = false,
                type = UpdateReleaseType.Alpha
            )
        )
    }

    @Test
    fun `preferred build uses alpha channel only when alpha updates are enabled`() {
        val releaseInfo = updateBuildInfo(UpdateReleaseType.Release, revision = 999)
        val alphaInfo = updateBuildInfo(UpdateReleaseType.Alpha, revision = 935)

        assertEquals(
            alphaInfo,
            selectPreferredUpdateBuild(
                releaseInfo = releaseInfo,
                alphaInfo = alphaInfo,
                includeAlpha = true
            )
        )
    }

    @Test
    fun `preferred build uses release channel only when alpha updates are disabled`() {
        val releaseInfo = updateBuildInfo(UpdateReleaseType.Release, revision = 934)
        val alphaInfo = updateBuildInfo(UpdateReleaseType.Alpha, revision = 999)

        assertEquals(
            releaseInfo,
            selectPreferredUpdateBuild(
                releaseInfo = releaseInfo,
                alphaInfo = alphaInfo,
                includeAlpha = false
            )
        )
    }

    @Test
    fun `preferred build fails when selected alpha channel has no build`() {
        val releaseInfo = updateBuildInfo(UpdateReleaseType.Release, revision = 999)

        val error = assertFailsWith<IllegalStateException> {
            selectPreferredUpdateBuild(
                releaseInfo = releaseInfo,
                alphaInfo = null,
                includeAlpha = true
            )
        }

        assertEquals("No alpha update build found", error.message)
    }

    @Test
    fun `github release page parser extracts tag name from og url`() {
        val html = """
            <html>
              <head>
                <meta property="og:url" content="/b1ackmarket/NeoBV/releases/tag/v0.4.0" />
              </head>
            </html>
        """.trimIndent()

        assertEquals("v0.4.0", parseGithubReleaseTagName(html))
    }

    @Test
    fun `github release page parser extracts release name from title`() {
        val html = """
            <title>Release 0.4.0.r934.0618869d.release · b1ackmarket/NeoBV · GitHub</title>
        """.trimIndent()

        assertEquals("0.4.0.r934.0618869d.release", parseGithubReleaseName(html))
    }

    @Test
    fun `github expanded assets parser extracts release download assets`() {
        val html = """
            <a href="/b1ackmarket/NeoBV/releases/download/v0.4.0/mapping.zip">mapping.zip</a>
            <a href="/b1ackmarket/NeoBV/releases/download/v0.4.0/NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk">apk</a>
        """.trimIndent()

        val assets = parseGithubReleaseAssets("b1ackmarket", "NeoBV", html)

        assertEquals(
            listOf(
                "mapping.zip",
                "NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk"
            ),
            assets.map { it.name }
        )
        assertEquals(
            "https://github.com/b1ackmarket/NeoBV/releases/download/v0.4.0/NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk",
            assets[1].browserDownloadUrl
        )
    }

    @Test
    fun `github page release can produce update build info`() {
        val releasePage = """
            <title>Release 0.4.0.r934.0618869d.release · b1ackmarket/NeoBV · GitHub</title>
        """.trimIndent()
        val assetsPage = """
            <a href="/b1ackmarket/NeoBV/releases/download/v0.4.0/NeoBV_934_0.4.0.r934.0618869d.release_default_universal.apk">apk</a>
        """.trimIndent()

        val release = buildGithubReleaseFromPages(
            owner = "b1ackmarket",
            repo = "NeoBV",
            tagName = "v0.4.0",
            releasePage = releasePage,
            assetsPage = assetsPage
        )
        val assetName = selectUpdateApkAssetName(release.assets.map { it.name }, isDebugBuild = false)

        assertEquals("0.4.0.r934.0618869d.release", release.name)
        assertNotNull(assetName)
        assertEquals(934, parseUpdateApkRevision(assetName))
    }

    @Test
    fun `github alpha page release can produce alpha asset info`() {
        val releasePage = """
            <title>Release 0.4.0.r935.7b8020ee.alpha · b1ackmarket/NeoBV · GitHub</title>
        """.trimIndent()
        val assetsPage = """
            <a href="/b1ackmarket/NeoBV/releases/download/alpha/NeoBV_935_0.4.0.r935.7b8020ee.alpha_default_universal.apk">apk</a>
            <a href="/b1ackmarket/NeoBV/releases/download/alpha/NeoBV_935_0.4.0.r935.7b8020ee.release_default_universal.apk">wrong channel</a>
        """.trimIndent()

        val release = buildGithubReleaseFromPages(
            owner = "b1ackmarket",
            repo = "NeoBV",
            tagName = "alpha",
            releasePage = releasePage,
            assetsPage = assetsPage,
            prerelease = true
        )
        val assetName = selectUpdateApkAssetName(
            release.assets.map { it.name },
            isDebugBuild = false,
            type = UpdateReleaseType.Alpha
        )

        assertEquals("0.4.0.r935.7b8020ee.alpha", release.name)
        assertEquals(true, release.prerelease)
        assertEquals("NeoBV_935_0.4.0.r935.7b8020ee.alpha_default_universal.apk", assetName)
        assertEquals(935, parseUpdateApkRevision(assetName!!))
    }

    private fun updateBuildInfo(type: UpdateReleaseType, revision: Int): UpdateBuildInfo {
        return UpdateBuildInfo(
            release = buildGithubReleaseFromPages(
                owner = "b1ackmarket",
                repo = "NeoBV",
                tagName = "v0.4.0",
                releasePage = "<title>Release test · b1ackmarket/NeoBV · GitHub</title>",
                assetsPage = ""
            ),
            type = type,
            revision = revision,
            assetName = when (type) {
                UpdateReleaseType.Release -> "NeoBV_${revision}_0.4.0.r$revision.0618869d.release_default_universal.apk"
                UpdateReleaseType.Alpha -> "NeoBV_${revision}_0.4.0.r$revision.0618869d.alpha_default_universal.apk"
            }
        )
    }
}
