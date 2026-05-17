package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.entity.Release
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal object GithubRepositoryConfig {
    const val OWNER = "b1ackmarket"
    const val REPO = "NeoBV"
}

enum class UpdateReleaseType {
    Release,
    Alpha
}

data class UpdateBuildInfo(
    val release: Release,
    val type: UpdateReleaseType,
    val revision: Int,
    val assetName: String
)

object GithubApi {
    private var endPoint = "api.github.com"
    private lateinit var client: HttpClient
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = endPoint
                }
            }
        }
    }

    private suspend fun getReleases(
        owner: String = GithubRepositoryConfig.OWNER,
        repo: String = GithubRepositoryConfig.REPO,
        pageSize: Int = 30,
        page: Int = 1
    ): List<Release> {
        val response = client.get(toGhProxyUrl("$GithubApiBase/repos/$owner/$repo/releases")) {
            parameter("per_page", pageSize)
            parameter("page", page)
        }.bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<List<Release>>(response)
    }

    private suspend fun getLatestReleaseFromApi(
        owner: String = GithubRepositoryConfig.OWNER,
        repo: String = GithubRepositoryConfig.REPO
    ): Release {
        val response = client.get(toGhProxyUrl("$GithubApiBase/repos/$owner/$repo/releases/latest")).bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<Release>(response)
    }

    private suspend fun getLatestReleaseFromGithubPages(
        owner: String = GithubRepositoryConfig.OWNER,
        repo: String = GithubRepositoryConfig.REPO
    ): Release {
        val releasePageUrl = "$GithubWebBase/$owner/$repo/releases/latest"
        val releasePage = client.get(toGhProxyUrl(releasePageUrl)).bodyAsText()
        val tagName = parseGithubReleaseTagName(releasePage)
            ?: throw IllegalStateException("Release tag not found")
        return getReleaseFromGithubPages(
            owner = owner,
            repo = repo,
            tagName = tagName,
            releasePage = releasePage
        )
    }

    private suspend fun getReleaseFromGithubPages(
        owner: String = GithubRepositoryConfig.OWNER,
        repo: String = GithubRepositoryConfig.REPO,
        tagName: String,
        releasePage: String? = null,
        prerelease: Boolean = false
    ): Release {
        val resolvedReleasePage = releasePage
            ?: client.get(toGhProxyUrl("$GithubWebBase/$owner/$repo/releases/tag/$tagName")).bodyAsText()
        val assetsPageUrl = "$GithubWebBase/$owner/$repo/releases/expanded_assets/$tagName"
        val assetsPage = client.get(toGhProxyUrl(assetsPageUrl)).bodyAsText()
        return buildGithubReleaseFromPages(
            owner = owner,
            repo = repo,
            tagName = tagName,
            releasePage = resolvedReleasePage,
            assetsPage = assetsPage,
            prerelease = prerelease
        )
    }

    suspend fun getLatestPreReleaseBuild(): Release =
        getReleaseFromGithubPages(tagName = GithubAlphaTag, prerelease = true)

    suspend fun getLatestReleaseBuild(): Release = getLatestReleaseFromGithubPages()

    suspend fun getPreferredBuild(includeAlpha: Boolean): UpdateBuildInfo {
        val releaseInfo = if (includeAlpha) {
            null
        } else {
            runCatching { getLatestReleaseBuild().toUpdateBuildInfo(UpdateReleaseType.Release) }.getOrNull()
        }
        val alphaInfo = if (includeAlpha) {
            runCatching { getLatestPreReleaseBuild().toUpdateBuildInfo(UpdateReleaseType.Alpha) }.getOrNull()
        } else {
            null
        }

        return selectPreferredUpdateBuild(
            releaseInfo = releaseInfo,
            alphaInfo = alphaInfo,
            includeAlpha = includeAlpha
        )
    }

    private fun checkErrorMessage(data: String) {
        val responseElement = json.parseToJsonElement(data)
        if (responseElement !is JsonObject) return
        val responseObject = responseElement.jsonObject
        check(responseObject.size != 2 && responseObject["message"] == null) { responseObject["message"]!!.jsonPrimitive.content }
    }

    suspend fun downloadUpdate(
        buildInfo: UpdateBuildInfo,
        file: File,
        downloadListener: ProgressListener
    ) {
        val downloadUrl = buildInfo.release.assets.firstOrNull {
            it.name == buildInfo.assetName
        }?.browserDownloadUrl
        downloadUrl ?: throw IllegalStateException("Didn't find download url")
        client.prepareRequest {
            // 通过代理进行下载
            url(toGhProxyUrl(downloadUrl))
            onDownload(downloadListener)
        }.execute { response ->
            response.bodyAsChannel().copyAndClose(file.writeChannel())
        }
    }
}

internal fun selectPreferredUpdateBuild(
    releaseInfo: UpdateBuildInfo?,
    alphaInfo: UpdateBuildInfo?,
    includeAlpha: Boolean
): UpdateBuildInfo {
    val selectedBuildInfo = if (includeAlpha) alphaInfo else releaseInfo
    return selectedBuildInfo
        ?: throw IllegalStateException(if (includeAlpha) "No alpha update build found" else "No release update build found")
}

private fun Release.toUpdateBuildInfo(type: UpdateReleaseType): UpdateBuildInfo? {
    val assetName = selectUpdateApkAssetName(
        assetNames = assets.map { it.name },
        isDebugBuild = BuildConfig.DEBUG,
        type = type
    )
        ?: return null
    val revision = parseUpdateApkRevision(assetName) ?: return null
    return UpdateBuildInfo(
        release = this,
        type = type,
        revision = revision,
        assetName = assetName
    )
}

private const val GhFastPrefix = "https://ghfast.top/"
private const val GithubApiBase = "https://api.github.com"
private const val GithubWebBase = "https://github.com"
private const val GithubAlphaTag = "alpha"

internal fun toGhProxyUrl(originalUrl: String): String {
    return if (originalUrl.startsWith(GhFastPrefix)) originalUrl else GhFastPrefix + originalUrl
}

internal fun parseGithubReleaseTagName(releasePage: String): String? {
    val ogUrlPattern = Regex("""<meta property="og:url" content="[^"]*/releases/tag/([^"]+)"""")
    val tagPattern = Regex("""/releases/tag/([^"?#<\s]+)""")
    return ogUrlPattern.find(releasePage)?.groupValues?.get(1)
        ?: tagPattern.find(releasePage)?.groupValues?.get(1)
}

internal fun parseGithubReleaseName(releasePage: String): String? {
    return Regex("""<title>\s*Release\s+(.+?)\s+·""", RegexOption.DOT_MATCHES_ALL)
        .find(releasePage)
        ?.groupValues
        ?.get(1)
        ?.htmlUnescape()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

internal fun parseGithubReleaseAssets(
    owner: String,
    repo: String,
    assetsPage: String
): List<Release.Asset> {
    val hrefPrefix = "/$owner/$repo/releases/download/"
    val pattern = Regex("""href="$hrefPrefix([^"/]+)/([^"]+)"""")
    return pattern.findAll(assetsPage)
        .map { match ->
            val tagName = match.groupValues[1]
            val assetName = match.groupValues[2].htmlUnescape()
            Release.Asset(
                browserDownloadUrl = "$GithubWebBase/$owner/$repo/releases/download/$tagName/$assetName",
                contentType = when {
                    assetName.endsWith(".apk", ignoreCase = true) -> "application/vnd.android.package-archive"
                    assetName.endsWith(".zip", ignoreCase = true) -> "application/zip"
                    else -> "application/octet-stream"
                },
                createdAt = "",
                downloadCount = 0,
                id = 0,
                label = "",
                name = assetName,
                nodeId = "",
                size = 0,
                state = "uploaded",
                updatedAt = "",
                uploader = emptyGithubUser(),
                url = ""
            )
        }
        .toList()
}

internal fun buildGithubReleaseFromPages(
    owner: String,
    repo: String,
    tagName: String,
    releasePage: String,
    assetsPage: String,
    prerelease: Boolean = false
): Release {
    val htmlUrl = "$GithubWebBase/$owner/$repo/releases/tag/$tagName"
    return Release(
        assets = parseGithubReleaseAssets(owner, repo, assetsPage),
        assetsUrl = "",
        author = emptyGithubUser(),
        body = "通过 GitHub 发布页查看更新内容：$htmlUrl",
        createdAt = "",
        draft = false,
        htmlUrl = htmlUrl,
        id = 0,
        name = parseGithubReleaseName(releasePage) ?: tagName,
        nodeId = "",
        prerelease = prerelease,
        publishedAt = "",
        reactions = null,
        tagName = tagName,
        tarballUrl = "",
        targetCommitish = "",
        uploadUrl = "",
        url = htmlUrl,
        zipballUrl = ""
    )
}

private fun emptyGithubUser(): Release.User {
    return Release.User(
        avatarUrl = "",
        eventsUrl = "",
        followersUrl = "",
        followingUrl = "",
        gistsUrl = "",
        gravatarId = "",
        htmlUrl = "",
        id = 0,
        login = "",
        nodeId = "",
        organizationsUrl = "",
        receivedEventsUrl = "",
        reposUrl = "",
        siteAdmin = false,
        starredUrl = "",
        subscriptionsUrl = "",
        type = "",
        url = ""
    )
}

private fun String.htmlUnescape(): String {
    return replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}
