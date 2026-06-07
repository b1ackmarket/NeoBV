package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.danmaku.DanmakuFilterConfig
import dev.aaa1115910.bv.danmaku.cacheCloudDanmakuFilterRules
import dev.aaa1115910.bv.danmaku.currentDanmakuFilterUid
import dev.aaa1115910.bv.danmaku.readDanmakuFilterConfigFromPrefs
import dev.aaa1115910.bv.danmaku.writeDanmakuFilterConfigToPrefs
import dev.aaa1115910.bv.plugin.impl.sponsorblock.PrefsSponsorBlockConfigStore
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SkipPolicy
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SponsorBlockCategoryStyle
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SponsorBlockConfig
import dev.aaa1115910.bv.repository.LiveRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bv.subtitle.translation.DefaultSubtitleTranslationPrompt
import dev.aaa1115910.bv.subtitle.translation.DefaultSubtitleTranslationTestSentence
import dev.aaa1115910.bv.subtitle.translation.SubtitleLanguages
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationProviderType
import dev.aaa1115910.bv.subtitle.translation.readSubtitleTranslationConfigFromPrefs
import dev.aaa1115910.bv.subtitle.translation.testSubtitleTranslationConnection
import dev.aaa1115910.bv.subtitle.translation.writeSubtitleTranslationConfigToPrefs
import dev.aaa1115910.bv.util.LayoutConfig
import dev.aaa1115910.bv.util.LogCatcherUtil
import dev.aaa1115910.bv.util.Prefs
import io.ktor.http.Parameters
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.FileNotFoundException
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object HttpServer {
    private const val SERVER_PORT = 2944
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var currentMpdContent: String? = null
    private val _searchInputFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val searchInputFlow = _searchInputFlow.asSharedFlow()

    fun startServer() {
        if (server != null) return
        val newServer = embeddedServer(CIO, port = SERVER_PORT) {
            homeModule()
            searchInputModule()
            logsUiStaticModule()
            logsApiModule()
            sponsorBlockModule()
            danmakuFilterModule()
            subtitleModule()
            layoutModule()
        }
        try {
            newServer.start(wait = false)
            server = newServer
        } catch (t: Throwable) {
            runCatching { newServer.stop(gracePeriodMillis = 0, timeoutMillis = 0) }
            throw t
        }
    }

    fun stopServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        server = null
        currentMpdContent = null
    }

    fun getServerAddress(path: String = "/"): String {
        val host = getLocalIpv4Address()
        return "http://$host:$SERVER_PORT$path"
    }

    fun setMpdContent(content: String) {
        currentMpdContent = content
    }

    fun getMpdUrl(): String = getServerAddress("/video.mpd")

    fun getSearchInputUrl(): String = getServerAddress("/input")

    private fun Application.homeModule() {
        routing {
            get("/video.mpd") {
                val mpd = currentMpdContent ?: return@get call.respondText(
                    text = "no mpd content",
                    status = HttpStatusCode.NotFound
                )
                call.respondText(
                    text = mpd,
                    contentType = ContentType.Application.Xml.withCharset(Charsets.UTF_8)
                )
            }
            // 打开网页只需要 http://ip:port/ ，直接返回日志管理首页
            get("/") {
                val bytes = readAssetBytesOrNull("logs_ui/index.html")
                    ?: return@get call.respondText(
                        text = "logs_ui/index.html not found in assets",
                        status = HttpStatusCode.NotFound
                    )
                call.respondBytes(bytes, contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8))
            }
        }
    }

    private fun Application.searchInputModule() {
        routing {
            get("/input") {
                call.respondText(
                    text = searchInputHtml(),
                    contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                )
            }
            post("/api/search/input") {
                val body = call.receiveText()
                val keyword = parseFormBody(body)["keyword"]
                    ?.trim()
                    ?.take(200)
                    .orEmpty()
                if (keyword.isBlank()) {
                    return@post call.respondText(
                        text = """{"success":false,"error":"empty keyword"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                }
                _searchInputFlow.tryEmit(keyword)
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }
        }
    }

    private fun Application.logsUiStaticModule() {
        routing {
            // 静态资源：/logs_ui/xxx 从 assets/logs_ui/xxx 读取
            get("/logs_ui/{path...}") {
                val segments = call.parameters.getAll("path").orEmpty()
                val relPath = segments.joinToString("/").ifBlank { "index.html" }

                // 简单防穿越：拒绝 .. 和 Windows 分隔符
                if (relPath.contains("..") || relPath.contains("\\")) {
                    return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                }

                val assetPath = "logs_ui/$relPath"
                val bytes = readAssetBytesOrNull(assetPath)
                    ?: return@get call.respondText(
                        text = "not found",
                        status = HttpStatusCode.NotFound
                    )
                call.respondBytes(bytes, contentType = contentTypeFor(relPath))
            }
        }
    }

    private fun Application.logsApiModule() {
        routing {
            // 1) 旧接口保留：下载日志文件，但增加白名单校验（失败 403）
            get("/api/logs/{filename}") {
                val filename =
                    call.parameters["filename"] ?: return@get call.respondText(
                        text = "filename is null",
                        status = HttpStatusCode.NotFound
                    )

                // 拒绝路径穿越
                if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                    return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                }

                // 仅允许 logs_manual_*.log / logs_crash_*.log
                val allowedPrefix =
                    filename.startsWith("logs_manual_") || filename.startsWith("logs_crash_")
                val allowedSuffix = filename.endsWith(".log")
                if (!allowedPrefix || !allowedSuffix) {
                    return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                }

                LogCatcherUtil.updateLogFiles()
                val file = (LogCatcherUtil.crashFiles + LogCatcherUtil.manualFiles)
                    .find { it.name == filename } ?: return@get call.respondText(
                    text = "file not found",
                    status = HttpStatusCode.NotFound
                )

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        file.name
                    ).toString()
                )
                call.respondFile(file)
            }

            // 2) 新增：列出日志文件 JSON
            get("/api/logs/list") {
                LogCatcherUtil.updateLogFiles()
                val manual = LogCatcherUtil.manualFiles
                val crash = LogCatcherUtil.crashFiles

                val items = (manual + crash)
                    .sortedByDescending { it.lastModified() }
                    .map { file ->
                        val type = when {
                            file.name.startsWith("logs_manual_") -> "manual"
                            file.name.startsWith("logs_crash_") -> "crash"
                            else -> "unknown"
                        }
                        LogItem(
                            name = file.name,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            type = type
                        )
                    }

                call.respondText(
                    text = itemsToJson(items),
                    contentType = ContentType.Application.Json
                )
            }

            // 3) 新增：手动保存并立刻下载
            get("/api/logs/create-manual-and-download") {
                val file = LogCatcherUtil.logLogcat(manual = true)
                if (file == null || !file.exists()) {
                    return@get call.respondText(
                        text = "create manual log failed",
                        status = HttpStatusCode.InternalServerError
                    )
                }

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        file.name
                    ).toString()
                )
                call.respondFile(file)
            }
        }
    }

    private fun Application.sponsorBlockModule() {
        routing {
            get("/api/plugins/sponsorblock/status") {
                val store = PrefsSponsorBlockConfigStore()
                val enabled = runBlocking { store.isEnabled() }
                val host = getLocalIpv4Address()
                call.respondText(
                    text = """{"enabled":$enabled,"address":"http://$host:$SERVER_PORT/sponsorblock"}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/api/plugins/sponsorblock/config") {
                val store = PrefsSponsorBlockConfigStore()
                val config = runBlocking {
                    val enabled = store.isEnabled()
                    store.readConfig().copy(enabled = enabled)
                }
                call.respondText(
                    text = config.toJson(),
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/plugins/sponsorblock/config") {
                val body = call.receiveText()
                val newConfig = parseSponsorBlockConfig(body) ?: return@post call.respondText(
                    text = """{"error":"invalid config"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                val store = PrefsSponsorBlockConfigStore()
                runBlocking {
                    val enabled = store.isEnabled()
                    store.writeConfig(newConfig.copy(enabled = enabled))
                }
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/sponsorblock") {
                val store = PrefsSponsorBlockConfigStore()
                val config = runBlocking {
                    val enabled = store.isEnabled()
                    store.readConfig().copy(enabled = enabled)
                }
                val enabledStatusText = if (config.enabled) "已启用" else "未启用"
                val categoryRows = SponsorBlockConfig.supportedCategories.joinToString("\n") { category ->
                    val displayName = sponsorBlockCategoryDisplayName(category)
                    val description = sponsorBlockCategoryDescription(category)
                    val selected = config.categoryPolicy[category] ?: SkipPolicy.Disabled
                    val autoChecked = if (selected == SkipPolicy.Auto) "checked" else ""
                    val promptChecked = if (selected == SkipPolicy.Prompt) "checked" else ""
                    val disabledChecked = if (selected == SkipPolicy.Disabled) "checked" else ""
                    """
                        <div class="segment-card">
                          <div class="segment-header">
                            <div class="segment-dot dot-$category"></div>
                            <div class="segment-meta">
                              <div class="segment-title">$displayName</div>
                              <div class="segment-desc">$description</div>
                            </div>
                          </div>
                          <div class="segment-actions">
                            <label><input type="radio" name="policy_$category" value="Auto" $autoChecked /> 跳过一次</label>
                            <label><input type="radio" name="policy_$category" value="Prompt" $promptChecked /> 显示浮窗</label>
                            <label><input type="radio" name="policy_$category" value="Disabled" $disabledChecked /> 禁用</label>
                          </div>
                        </div>
                    """.trimIndent()
                }
                call.respondText(
                    text = """
                        <!DOCTYPE html>
                        <html lang="zh-CN">
                        <head>
                          <meta charset="utf-8" />
                          <meta name="viewport" content="width=device-width, initial-scale=1" />
                          <title>SponsorBlock 配置</title>
                          <style>
                            :root {
                              --bg: #111214;
                              --panel: #1a1c20;
                              --panel-2: #22252b;
                              --border: rgba(255,255,255,0.08);
                              --muted: #a5adba;
                              --text: #f6f7fb;
                              --accent: #8ea8ff;
                            }
                            * { box-sizing: border-box; }
                            body {
                              margin: 0;
                              background: var(--bg);
                              color: var(--text);
                              font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif;
                            }
                            .page {
                              max-width: 880px;
                              margin: 0 auto;
                              padding: 24px 18px 48px;
                            }
                            .hero {
                              padding: 10px 4px 18px;
                            }
                            h1 {
                              margin: 0 0 8px;
                              font-size: 28px;
                            }
                            p {
                              margin: 0;
                              color: var(--muted);
                              line-height: 1.5;
                            }
                            .card {
                              background: var(--panel);
                              border: 1px solid var(--border);
                              border-radius: 20px;
                              overflow: hidden;
                              box-shadow: 0 18px 50px rgba(0,0,0,0.28);
                            }
                            .section {
                              padding: 18px 20px;
                              border-top: 1px solid var(--border);
                            }
                            .section:first-child {
                              border-top: none;
                            }
                            .section-title {
                              font-size: 18px;
                              font-weight: 700;
                              margin-bottom: 12px;
                            }
                            .toggle-row {
                              display: flex;
                              align-items: center;
                              justify-content: space-between;
                              gap: 12px;
                            }
                            .switch {
                              display: inline-flex;
                              align-items: center;
                              gap: 10px;
                              font-size: 15px;
                            }
                            .switch input {
                              width: 18px;
                              height: 18px;
                            }
                            .segment-list {
                              display: grid;
                              gap: 12px;
                            }
                            .segment-card {
                              background: var(--panel-2);
                              border: 1px solid var(--border);
                              border-radius: 16px;
                              padding: 14px 16px;
                            }
                            .segment-header {
                              display: flex;
                              align-items: flex-start;
                              gap: 12px;
                            }
                            .segment-dot {
                              width: 12px;
                              height: 12px;
                              border-radius: 999px;
                              margin-top: 5px;
                              flex: 0 0 auto;
                            }
                            .segment-meta {
                              min-width: 0;
                            }
                            .segment-title {
                              font-size: 17px;
                              font-weight: 700;
                              margin-bottom: 4px;
                            }
                            .segment-desc {
                              font-size: 14px;
                              color: var(--muted);
                              line-height: 1.45;
                            }
                            .segment-actions {
                              display: flex;
                              flex-wrap: wrap;
                              gap: 14px;
                              margin-top: 12px;
                              padding-left: 24px;
                            }
                            .segment-actions label {
                              display: inline-flex;
                              align-items: center;
                              gap: 6px;
                              font-size: 14px;
                            }
                            .segment-actions input {
                              width: 16px;
                              height: 16px;
                            }
                            .action-bar {
                              display: flex;
                              align-items: center;
                              justify-content: space-between;
                              gap: 12px;
                              margin-top: 18px;
                            }
                            button {
                              padding: 12px 20px;
                              border: none;
                              border-radius: 999px;
                              background: var(--accent);
                              color: #101322;
                              font-size: 15px;
                              font-weight: 700;
                              cursor: pointer;
                            }
                            #result {
                              font-size: 14px;
                              color: var(--muted);
                            }
                            ${SponsorBlockCategoryStyle.cssDotRules()}
                          </style>
                        </head>
                        <body>
                          <div class="page">
                            <div class="hero">
                              <h1>空降助手</h1>
                              <p>默认改为显示提示，不会自动跳过。你可以为每类片段单独设置为「跳过一次 / 显示浮窗 / 禁用」。</p>
                            </div>
                            <div class="card">
                              <div class="section">
                                <div class="section-title">基础设置</div>
                                <div class="toggle-row">
                                  <span>空降助手状态：$enabledStatusText</span>
                                  <span style="color: var(--muted);">启用/关闭请回到电视端设置；本页只保存分类细项。</span>
                                </div>
                              </div>
                              <div class="section">
                                <div class="section-title">片段策略</div>
                                <div class="segment-list">
                                  $categoryRows
                                </div>
                                <div class="action-bar">
                                  <button onclick="saveConfig()">保存配置</button>
                                  <p id="result"></p>
                                </div>
                              </div>
                            </div>
                          </div>
                          <script>
                            const categories = ${SponsorBlockConfig.supportedCategories.joinToString(
                                prefix = "[",
                                postfix = "]"
                            ) { "\"$it\"" }};

                            function getPolicy(category) {
                              const checked = document.querySelector(`input[name="policy_${'$'}{category}"]:checked`);
                              return checked ? checked.value : 'Disabled';
                            }

                            async function saveConfig() {
                              const payload = {
                                enabled: ${config.enabled},
                                categoryPolicy: Object.fromEntries(
                                  categories.map(category => [category, getPolicy(category)])
                                )
                              };
                              const res = await fetch('/api/plugins/sponsorblock/config', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(payload)
                              });
                              document.getElementById('result').textContent = res.ok ? '配置已保存' : '保存失败，请重试';
                            }
                          </script>
                        </body>
                        </html>
                    """.trimIndent(),
                    contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                )
            }
        }
    }

    private fun Application.subtitleModule() {
        routing {
            get("/api/subtitle/config") {
                call.respondText(
                    text = readSubtitleTranslationConfigFromPrefs().toJson(),
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/subtitle/test") {
                val body = call.receiveText()
                val testConfig = parseSubtitleTranslationConfig(body) ?: return@post call.respondText(
                    text = """{"error":"invalid config"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                val testSentence = parseSubtitleTranslationTestSentence(body)
                val result = testSubtitleTranslationConnection(testConfig, testSentence)
                if (result.isSuccess) {
                    call.respondText(
                        text = """{"success":true,"signature":"${jsonEscape(testConfig.configSignature())}","translation":"${jsonEscape(result.getOrThrow())}"}""",
                        contentType = ContentType.Application.Json
                    )
                } else {
                    call.respondText(
                        text = """{"success":false,"error":"${jsonEscape(result.exceptionOrNull()?.message ?: "test failed")}"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                }
            }

            post("/api/subtitle/config") {
                val body = call.receiveText()
                val newConfig = parseSubtitleTranslationConfig(body) ?: return@post call.respondText(
                    text = """{"error":"invalid config"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                val currentConfig = readSubtitleTranslationConfigFromPrefs()
                val serviceConfigUnchanged = newConfig.configSignature() == currentConfig.configSignature()
                writeSubtitleBehaviorPrefs(body)
                if (!newConfig.verified() && !serviceConfigUnchanged) {
                    return@post call.respondText(
                        text = """{"error":"请先测试连接"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                }
                if (newConfig.verified()) {
                    writeSubtitleTranslationConfigToPrefs(newConfig)
                }
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/subtitle") {
                call.respondText(
                    text = subtitleConfigHtml(),
                    contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                )
            }
        }
    }

    private fun Application.layoutModule() {
        routing {
            get("/api/layout/config") {
                if (!Prefs.enableLayoutWebConfig) {
                    return@get call.respondText(
                        text = """{"error":"layout web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                call.respondText(
                    text = layoutConfigJson(),
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/layout/config") {
                if (!Prefs.enableLayoutWebConfig) {
                    return@post call.respondText(
                        text = """{"error":"layout web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                val body = call.receiveText()
                val element = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    ?: return@post call.respondText(
                        text = """{"error":"invalid config"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                val reset = element["reset"]?.jsonPrimitive?.booleanOrNull == true
                if (reset) {
                    LayoutConfig.reset()
                } else {
                    val state = runCatching { json.decodeFromString<dev.aaa1115910.bv.util.LayoutConfigState>(body) }
                        .getOrNull()
                        ?: return@post call.respondText(
                            text = """{"error":"invalid config"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.BadRequest
                        )
                    LayoutConfig.write(state)
                }
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/layout") {
                if (!Prefs.enableLayoutWebConfig) {
                    return@get call.respondText(
                        text = layoutDisabledHtml(),
                        contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
                        status = HttpStatusCode.Forbidden
                    )
                }
                call.respondText(
                    text = layoutConfigHtml(),
                    contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                )
            }
        }
    }

    private fun Application.danmakuFilterModule() {
        routing {
            get("/api/danmaku/config") {
                if (!Prefs.enableDanmakuFilterWebConfig) {
                    return@get call.respondText(
                        text = """{"error":"danmaku filter web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                call.respondText(
                    text = readDanmakuFilterConfigFromPrefs().toJson(),
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/danmaku/config") {
                if (!Prefs.enableDanmakuFilterWebConfig) {
                    return@post call.respondText(
                        text = """{"error":"danmaku filter web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                val config = parseDanmakuFilterConfig(call.receiveText())
                    ?: return@post call.respondText(
                        text = """{"error":"invalid config"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                writeDanmakuFilterConfigToPrefs(config)
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/danmaku/sync") {
                if (!Prefs.enableDanmakuFilterWebConfig) {
                    return@post call.respondText(
                        text = """{"error":"danmaku filter web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                val uid = currentDanmakuFilterUid()
                    ?: return@post call.respondText(
                        text = """{"error":"请先登录 B 站账号"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Unauthorized
                    )
                val rules = runCatching {
                    BVApp.koinApplication.koin.get<VideoPlayRepository>()
                        .getDanmakuFilterRules()
                }.getOrElse { error ->
                    return@post call.respondText(
                        text = """{"error":"${jsonEscape(error.message ?: "同步失败")}"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
                cacheCloudDanmakuFilterRules(uid, rules)
                Prefs.syncCloudDanmakuFilter = true
                call.respondText(
                    text = """{"success":true,"count":${rules.size},"config":${readDanmakuFilterConfigFromPrefs().toJson()}}""",
                    contentType = ContentType.Application.Json
                )
            }

            post("/api/danmaku/upload") {
                if (!Prefs.enableDanmakuFilterWebConfig) {
                    return@post call.respondText(
                        text = """{"error":"danmaku filter web config disabled"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Forbidden
                    )
                }
                currentDanmakuFilterUid()
                    ?: return@post call.respondText(
                        text = """{"error":"请先登录 B 站账号"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Unauthorized
                    )
                val uploaded = runCatching {
                    BVApp.koinApplication.koin.get<VideoPlayRepository>()
                        .uploadLocalDanmakuFilterRules(
                            keywords = Prefs.localDanmakuFilterKeywords.toRuleLinesForServer(),
                            regexes = Prefs.localDanmakuFilterRegexes.toRuleLinesForServer()
                        )
                }.getOrElse { error ->
                    return@post call.respondText(
                        text = """{"error":"${jsonEscape(error.message ?: "上传失败")}"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
                call.respondText(
                    text = """{"success":true,"uploaded":$uploaded}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/danmaku") {
                if (!Prefs.enableDanmakuFilterWebConfig) {
                    return@get call.respondText(
                        text = danmakuFilterDisabledHtml(),
                        contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                    )
                }
                call.respondText(
                    text = danmakuFilterConfigHtml(),
                    contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
                )
            }
        }
    }

    private fun DanmakuFilterConfig.toJson(): String {
        return buildString {
            append('{')
            append("\"enabled\":").append(enabled).append(',')
            append("\"syncCloudRules\":").append(syncCloudRules).append(',')
            append("\"caseSensitive\":").append(caseSensitive).append(',')
            append("\"localKeywords\":\"").append(jsonEscape(localKeywords)).append("\",")
            append("\"localRegexes\":\"").append(jsonEscape(localRegexes)).append("\",")
            append("\"localUserHashes\":\"").append(jsonEscape(localUserHashes)).append("\",")
            append("\"login\":").append(currentDanmakuFilterUid() != null).append(',')
            append("\"uid\":").append(Prefs.uid.takeIf { it > 0L } ?: 0L).append(',')
            append("\"cloudSyncedAt\":").append(Prefs.cloudDanmakuFilterSyncedAt)
            append('}')
        }
    }

    private fun parseDanmakuFilterConfig(body: String): DanmakuFilterConfig? {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val current = readDanmakuFilterConfigFromPrefs()
        return DanmakuFilterConfig(
            enabled = parsed["enabled"]?.jsonPrimitive?.booleanOrNull ?: current.enabled,
            syncCloudRules = parsed["syncCloudRules"]?.jsonPrimitive?.booleanOrNull
                ?: current.syncCloudRules,
            caseSensitive = parsed["caseSensitive"]?.jsonPrimitive?.booleanOrNull
                ?: current.caseSensitive,
            localKeywords = parsed["localKeywords"]?.jsonPrimitive?.contentOrNull?.take(12000)
                ?: current.localKeywords,
            localRegexes = parsed["localRegexes"]?.jsonPrimitive?.contentOrNull?.take(12000)
                ?: current.localRegexes,
            localUserHashes = parsed["localUserHashes"]?.jsonPrimitive?.contentOrNull?.take(12000)
                ?: current.localUserHashes
        )
    }

    private fun danmakuFilterDisabledHtml(): String {
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>弹幕屏蔽未开启</title>
              <style>
                body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: #101318; color: #f4f7fb; font-family: system-ui, sans-serif; }
                .card { max-width: 520px; padding: 28px; border: 1px solid rgba(255,255,255,.12); border-radius: 18px; background: rgba(255,255,255,.06); line-height: 1.6; }
              </style>
            </head>
            <body><div class="card"><h1>弹幕屏蔽未开启</h1><p>请先在软件设置的“画面音频”里开启“弹幕屏蔽”。</p></div></body>
            </html>
        """.trimIndent()
    }

    private fun danmakuFilterConfigHtml(): String {
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>弹幕屏蔽</title>
              <style>
                :root { color-scheme: dark; --bg:#101318; --panel:#181d25; --panel2:#202733; --text:#f4f7fb; --muted:#aeb8c8; --accent:#8ee6d1; --danger:#ff8a8a; --border:rgba(255,255,255,.12); }
                * { box-sizing:border-box; }
                body { margin:0; background:var(--bg); color:var(--text); font-family:system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                main { width:min(980px, calc(100vw - 28px)); margin:28px auto; display:grid; gap:16px; }
                h1 { margin:0; font-size:30px; }
                p { margin:8px 0 0; color:var(--muted); line-height:1.55; }
                .grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:14px; }
                .card { background:linear-gradient(180deg, rgba(255,255,255,.07), rgba(255,255,255,.035)); border:1px solid var(--border); border-radius:18px; padding:16px; }
                .card h2 { margin:0 0 12px; font-size:18px; }
                label { display:flex; align-items:flex-start; gap:10px; margin:11px 0; color:var(--text); line-height:1.45; }
                label span { display:grid; gap:2px; }
                small { color:var(--muted); }
                textarea { width:100%; min-height:220px; resize:vertical; border:1px solid var(--border); border-radius:14px; background:var(--panel2); color:var(--text); padding:12px; line-height:1.45; font:15px ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
                button { border:0; border-radius:999px; padding:10px 16px; background:var(--accent); color:#071613; font-weight:800; cursor:pointer; }
                .actions { display:flex; justify-content:flex-end; align-items:center; gap:12px; flex-wrap:wrap; }
                #result { min-height:22px; color:var(--muted); }
                #result.error { color:var(--danger); }
              </style>
            </head>
            <body>
              <main>
                <section>
                  <h1>弹幕屏蔽</h1>
                  <p>配置会在播放视频时应用。云端同步需要登录 B 站账号，缓存按账号区分；上传按钮会把本地关键词和正则写入当前账号的云端屏蔽词库。</p>
                </section>
                <section class="grid">
                  <div class="card">
                    <h2>基础</h2>
                    <label><input id="enabled" type="checkbox"><span>启用弹幕屏蔽<small>关闭后本地和云端规则都不会生效。</small></span></label>
                    <label><input id="syncCloudRules" type="checkbox"><span>应用 B 站云端屏蔽词<small>开启后播放时使用已同步的账号云端规则；缓存过期或账号变化时才会重新拉取。</small></span></label>
                    <label><input id="caseSensitive" type="checkbox"><span>区分大小写<small>仅影响关键词和正则，本地规则每行一条。</small></span></label>
                  </div>
                  <div class="card">
                    <h2>本地关键词</h2>
                    <textarea id="localKeywords" placeholder="每行一个关键词"></textarea>
                  </div>
                  <div class="card">
                    <h2>本地正则</h2>
                    <textarea id="localRegexes" placeholder="每行一个正则表达式"></textarea>
                  </div>
                  <div class="card">
                    <h2>本地用户 Hash</h2>
                    <textarea id="localUserHashes" placeholder="每行一个发送者 hash"></textarea>
                  </div>
                </section>
                <section class="actions">
                  <div id="result"></div>
                  <button onclick="syncCloud()">同步云端到本地</button>
                  <button onclick="uploadLocal()">上传本地到云端</button>
                  <button onclick="save()">保存配置</button>
                </section>
              </main>
              <script>
                const ids = ['enabled','syncCloudRules','caseSensitive','localKeywords','localRegexes','localUserHashes'];
                const el = (id) => document.getElementById(id);
                let resultTimer = 0;
                function showResult(text, error = false) {
                  clearTimeout(resultTimer);
                  el('result').textContent = text;
                  el('result').className = error ? 'error' : '';
                  resultTimer = setTimeout(() => { el('result').textContent = ''; el('result').className = ''; }, 3000);
                }
                async function load() {
                  const response = await fetch('/api/danmaku/config');
                  if (!response.ok) throw new Error(await response.text());
                  const config = await response.json();
                  applyConfig(config);
                }
                function applyConfig(config) {
                  ids.forEach(id => {
                    const node = el(id);
                    if (!node) return;
                    if (node.type === 'checkbox') node.checked = Boolean(config[id]);
                    else node.value = config[id] || '';
                  });
                  if (!config.login) showResult('当前未登录 B 站账号，云端同步和上传不可用', true);
                }
                async function save(show = true) {
                  const config = {};
                  ids.forEach(id => {
                    const node = el(id);
                    config[id] = node.type === 'checkbox' ? node.checked : node.value;
                  });
                  const response = await fetch('/api/danmaku/config', {
                    method:'POST',
                    headers:{'Content-Type':'application/json'},
                    body:JSON.stringify(config)
                  });
                  if (!response.ok) {
                    showResult(await response.text(), true);
                    return false;
                  }
                  if (show) showResult('已保存，重新进入播放器后生效');
                  return true;
                }
                async function syncCloud() {
                  const response = await fetch('/api/danmaku/sync', { method:'POST' });
                  const text = await response.text();
                  if (!response.ok) return showResult(text, true);
                  const result = JSON.parse(text);
                  applyConfig(result.config);
                  showResult('已同步 ' + (result.count || 0) + ' 条云端规则');
                }
                async function uploadLocal() {
                  if (!await save(false)) return;
                  const response = await fetch('/api/danmaku/upload', { method:'POST' });
                  const text = await response.text();
                  if (!response.ok) return showResult(text, true);
                  const result = JSON.parse(text);
                  showResult('已上传 ' + (result.uploaded || 0) + ' 条本地规则');
                }
                load().catch(error => showResult(String(error), true));
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun layoutConfigJson(): String {
        val liveCategories = runCatching {
            runBlocking {
                BVApp.koinApplication.koin.get<LiveRepository>().getCategories()
            }
        }.getOrDefault(emptyList())
        val state = LayoutConfig.toEditableState(liveCategories)
        val groups = state.groups.entries.joinToString(",") { (groupId, items) ->
            val groupName = dev.aaa1115910.bv.util.LayoutConfigGroup.entries
                .firstOrNull { it.id == groupId }
                ?.displayName
                ?: groupId
            val itemJson = items.joinToString(",") { item ->
                """{"id":"${jsonEscape(item.id)}","label":"${jsonEscape(item.label)}","hidden":${item.hidden}}"""
            }
            """"${jsonEscape(groupId)}":{"label":"${jsonEscape(groupName)}","items":[$itemJson]}"""
        }
        return """{"groups":{$groups}}"""
    }

    private fun layoutDisabledHtml(): String {
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>布局自定义未开启</title>
              <style>
                body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: #101318; color: #f4f7fb; font-family: system-ui, sans-serif; }
                .card { max-width: 520px; padding: 28px; border: 1px solid rgba(255,255,255,.12); border-radius: 18px; background: rgba(255,255,255,.06); line-height: 1.6; }
              </style>
            </head>
            <body><div class="card"><h1>布局自定义未开启</h1><p>请先在软件设置的“界面设置”里开启“布局自定义”。</p></div></body>
            </html>
        """.trimIndent()
    }

    private fun layoutConfigHtml(): String {
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>布局自定义</title>
              <style>
                :root { color-scheme: dark; --bg:#101318; --panel:#181d25; --panel2:#202733; --text:#f4f7fb; --muted:#aeb8c8; --accent:#8ee6d1; --danger:#ff8a8a; --border:rgba(255,255,255,.12); }
                * { box-sizing: border-box; }
                body { margin:0; background:var(--bg); color:var(--text); font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                .page { width:min(1040px, calc(100vw - 28px)); margin:28px auto; display:grid; gap:18px; }
                .hero { display:flex; align-items:flex-end; justify-content:space-between; gap:18px; flex-wrap:wrap; }
                h1 { margin:0; font-size:30px; }
                p { margin:8px 0 0; color:var(--muted); }
                .groups { display:grid; gap:16px; }
                .card { background:linear-gradient(180deg, rgba(255,255,255,.07), rgba(255,255,255,.035)); border:1px solid var(--border); border-radius:18px; padding:16px; }
                .card h2 { margin:0 0 12px; font-size:18px; }
                .items { display:grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap:10px; }
                .item { display:grid; grid-template-columns:auto auto 1fr; align-items:center; gap:9px; padding:10px 11px; border:1px solid var(--border); border-radius:12px; background:var(--panel2); min-width:0; cursor:grab; touch-action:none; user-select:none; }
                .item:active { cursor:grabbing; }
                .item.hidden { opacity:.52; }
                .item.dragging { opacity:.42; border-color:var(--accent); transform:scale(.985); }
                .handle { color:var(--muted); font-weight:800; letter-spacing:2px; cursor:grab; padding:4px; }
                .name { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
                button { border:0; border-radius:999px; padding:8px 12px; background:var(--accent); color:#071613; font-weight:700; cursor:pointer; }
                button.secondary { background:#d1d7e8; }
                button.danger { background:var(--danger); color:#220707; }
                .bar { display:flex; gap:10px; flex-wrap:wrap; align-items:center; justify-content:space-between; }
                .actions { display:grid; justify-items:end; gap:8px; }
                .action-buttons { display:flex; gap:10px; flex-wrap:wrap; justify-content:flex-end; }
                #result { color:var(--muted); min-height:22px; text-align:right; }
                #result.error { color:var(--danger); }
              </style>
            </head>
            <body>
              <main class="page">
                <div class="hero">
                  <div>
                    <h1>布局自定义</h1>
                    <p>调整各页面 tab 的顺序，或隐藏暂时不用的入口。每组至少会保留一个入口。</p>
                  </div>
                  <div class="actions">
                    <div class="action-buttons">
                      <button class="secondary" onclick="save()">保存布局</button>
                      <button class="danger" onclick="resetLayout()">重置布局</button>
                    </div>
                    <div id="result"></div>
                  </div>
                </div>
                <div id="groups" class="groups"></div>
              </main>
              <script>
                const el = (id) => document.getElementById(id);
                let state = { groups: {} };
                let groupLabels = {};
                let resultTimer = 0;
                let dragging = null;
                const dragStartThreshold = 6;
                function showResult(text, error = false) {
                  clearTimeout(resultTimer);
                  el('result').textContent = text;
                  el('result').className = error ? 'error' : '';
                  resultTimer = setTimeout(() => { el('result').textContent = ''; el('result').className = ''; }, 3000);
                }
                function toggle(groupId, index) {
                  const items = state.groups[groupId];
                  if (!items) return;
                  const visibleCount = items.filter(item => !item.hidden).length;
                  if (!items[index].hidden && visibleCount <= 1) {
                    showResult('每组至少保留一个入口', true);
                    return;
                  }
                  items[index].hidden = !items[index].hidden;
                  render();
                }
                function escapeHtml(text) {
                  return String(text).replace(/[&<>"']/g, (char) => ({
                    '&': '&amp;',
                    '<': '&lt;',
                    '>': '&gt;',
                    '"': '&quot;',
                    "'": '&#39;'
                  }[char]));
                }
                function reorder(groupId, fromIndex, toIndex) {
                  const items = state.groups[groupId];
                  if (!items || fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;
                  if (fromIndex >= items.length || toIndex >= items.length) return;
                  const [item] = items.splice(fromIndex, 1);
                  items.splice(toIndex, 0, item);
                  render();
                }
                function pointerItemFromEvent(event) {
                  const element = document.elementFromPoint(event.clientX, event.clientY);
                  return element ? element.closest('.item') : null;
                }
                function startPointerDrag(event, groupId, index, row) {
                  if (event.target && event.target.tagName === 'INPUT') return;
                  dragging = {
                    groupId,
                    index,
                    pointerId: event.pointerId,
                    startX: event.clientX,
                    startY: event.clientY,
                    active: false
                  };
                  row.setPointerCapture?.(event.pointerId);
                  document.addEventListener('pointermove', movePointerDrag, { passive:false });
                  document.addEventListener('pointerup', endPointerDrag);
                  document.addEventListener('pointercancel', endPointerDrag);
                }
                function movePointerDrag(event) {
                  if (!dragging || dragging.pointerId !== event.pointerId) return;
                  const distance = Math.hypot(event.clientX - dragging.startX, event.clientY - dragging.startY);
                  if (!dragging.active && distance < dragStartThreshold) return;
                  dragging.active = true;
                  event.preventDefault();
                  const current = document.querySelector('.item[data-group-id="' + dragging.groupId + '"][data-index="' + dragging.index + '"]');
                  current?.classList.add('dragging');
                  const target = pointerItemFromEvent(event);
                  if (!target || target.dataset.groupId !== dragging.groupId) return;
                  const targetIndex = Number(target.dataset.index);
                  if (!Number.isFinite(targetIndex) || targetIndex === dragging.index) return;
                  const groupId = dragging.groupId;
                  reorder(groupId, dragging.index, targetIndex);
                  dragging = {
                    ...dragging,
                    index: targetIndex
                  };
                  const moved = document.querySelector('.item[data-group-id="' + groupId + '"][data-index="' + targetIndex + '"]');
                  moved?.classList.add('dragging');
                }
                function endPointerDrag(event) {
                  if (!dragging || dragging.pointerId !== event.pointerId) return;
                  dragging = null;
                  document.removeEventListener('pointermove', movePointerDrag);
                  document.removeEventListener('pointerup', endPointerDrag);
                  document.removeEventListener('pointercancel', endPointerDrag);
                  document.querySelectorAll('.item.dragging').forEach(item => item.classList.remove('dragging'));
                }
                function render() {
                  el('groups').innerHTML = '';
                  Object.keys(state.groups).forEach(groupId => {
                    const card = document.createElement('section');
                    card.className = 'card';
                    const title = document.createElement('h2');
                    title.textContent = groupLabels[groupId] || groupId;
                    card.appendChild(title);
                    const list = document.createElement('div');
                    list.className = 'items';
                    state.groups[groupId].forEach((item, index) => {
                      const row = document.createElement('div');
                      row.className = 'item' + (item.hidden ? ' hidden' : '');
                      row.dataset.groupId = groupId;
                      row.dataset.index = String(index);
                      row.innerHTML =
                        '<input type="checkbox" ' + (item.hidden ? '' : 'checked') + ' aria-label="显示" />' +
                        '<div class="handle" aria-hidden="true">::</div>' +
                        '<div class="name" title="' + escapeHtml(item.label) + '">' + escapeHtml(item.label) + '</div>';
                      row.querySelector('input').addEventListener('change', () => toggle(groupId, index));
                      row.addEventListener('pointerdown', (event) => {
                        startPointerDrag(event, groupId, index, row);
                      });
                      list.appendChild(row);
                    });
                    card.appendChild(list);
                    el('groups').appendChild(card);
                  });
                }
                async function load() {
                  const response = await fetch('/api/layout/config');
                  if (!response.ok) throw new Error(await response.text());
                  const payload = await response.json();
                  groupLabels = {};
                  state = { groups: {} };
                  Object.entries(payload.groups || {}).forEach(([groupId, group]) => {
                    groupLabels[groupId] = group.label || groupId;
                    state.groups[groupId] = (group.items || []).map(item => ({ id:item.id, label:item.label, hidden:Boolean(item.hidden) }));
                  });
                  render();
                }
                async function save() {
                  const response = await fetch('/api/layout/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(state)
                  });
                  if (!response.ok) return showResult(await response.text(), true);
                  showResult('已保存，重启应用或重新进入对应页面后生效');
                }
                async function resetLayout() {
                  const response = await fetch('/api/layout/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ reset: true })
                  });
                  if (!response.ok) return showResult(await response.text(), true);
                  await load();
                  showResult('已重置布局');
                }
                load().catch(error => showResult(String(error), true));
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun SubtitleTranslationConfig.toJson(): String {
        return buildString {
            append('{')
            append("\"preferBilingualSubtitleOnOsd\":").append(Prefs.preferBilingualSubtitleOnOsd).append(',')
            append("\"preferCustomSecondarySubtitle\":").append(Prefs.preferCustomSecondarySubtitle).append(',')
            append("\"providerType\":\"").append(providerType.name).append("\",")
            append("\"targetLanguage\":\"").append(jsonEscape(targetLanguage)).append("\",")
            append("\"contextBefore\":").append(contextBefore).append(',')
            append("\"contextAfter\":").append(contextAfter).append(',')
            append("\"requestBatchSize\":").append(requestBatchSize).append(',')
            append("\"preTranslateSeconds\":").append(preTranslateSeconds).append(',')
            append("\"openAiBaseUrl\":\"").append(jsonEscape(openAiBaseUrl)).append("\",")
            append("\"openAiApiKey\":\"").append(jsonEscape(openAiApiKey)).append("\",")
            append("\"openAiModel\":\"").append(jsonEscape(openAiModel)).append("\",")
            append("\"openAiPrompt\":\"").append(jsonEscape(openAiPrompt)).append("\",")
            append("\"baiduAppId\":\"").append(jsonEscape(baiduAppId)).append("\",")
            append("\"baiduAppKey\":\"").append(jsonEscape(baiduAppKey)).append("\",")
            append("\"microsoftKey\":\"").append(jsonEscape(microsoftKey)).append("\",")
            append("\"microsoftRegion\":\"").append(jsonEscape(microsoftRegion)).append("\",")
            append("\"microsoftEndpoint\":\"").append(jsonEscape(microsoftEndpoint)).append("\",")
            append("\"deepLApiKey\":\"").append(jsonEscape(deepLApiKey)).append("\",")
            append("\"deepLEndpoint\":\"").append(jsonEscape(deepLEndpoint)).append("\",")
            append("\"deepLXEndpoint\":\"").append(jsonEscape(deepLXEndpoint)).append("\",")
            append("\"deepLXApiKey\":\"").append(jsonEscape(deepLXApiKey)).append("\",")
            append("\"verifiedSignature\":\"").append(jsonEscape(verifiedSignature)).append("\",")
            append("\"currentSignature\":\"").append(jsonEscape(configSignature())).append("\",")
            append("\"verified\":").append(verified())
            append('}')
        }
    }

    private fun parseSubtitleTranslationConfig(body: String): SubtitleTranslationConfig? {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val current = readSubtitleTranslationConfigFromPrefs()
        return SubtitleTranslationConfig(
            providerType = parsed["providerType"]?.jsonPrimitive?.contentOrNull
                ?.let(SubtitleTranslationProviderType::fromName)
                ?: current.providerType,
            targetLanguage = parsed["targetLanguage"]?.jsonPrimitive?.contentOrNull?.take(20)
                ?: current.targetLanguage,
            contextBefore = parsed["contextBefore"]?.jsonPrimitive?.intOrNull ?: current.contextBefore,
            contextAfter = parsed["contextAfter"]?.jsonPrimitive?.intOrNull ?: current.contextAfter,
            requestBatchSize = parsed["requestBatchSize"]?.jsonPrimitive?.intOrNull
                ?: current.requestBatchSize,
            preTranslateSeconds = parsed["preTranslateSeconds"]?.jsonPrimitive?.intOrNull
                ?: current.preTranslateSeconds,
            openAiBaseUrl = parsed["openAiBaseUrl"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.openAiBaseUrl,
            openAiApiKey = parsed["openAiApiKey"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.openAiApiKey,
            openAiModel = parsed["openAiModel"]?.jsonPrimitive?.contentOrNull?.take(120)
                ?: current.openAiModel,
            openAiPrompt = parsed["openAiPrompt"]?.jsonPrimitive?.contentOrNull?.take(5000)
                ?: current.openAiPrompt,
            baiduAppId = parsed["baiduAppId"]?.jsonPrimitive?.contentOrNull?.take(200)
                ?: current.baiduAppId,
            baiduAppKey = parsed["baiduAppKey"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.baiduAppKey,
            microsoftKey = parsed["microsoftKey"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.microsoftKey,
            microsoftRegion = parsed["microsoftRegion"]?.jsonPrimitive?.contentOrNull?.take(80)
                ?: current.microsoftRegion,
            microsoftEndpoint = parsed["microsoftEndpoint"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.microsoftEndpoint,
            deepLApiKey = parsed["deepLApiKey"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.deepLApiKey,
            deepLEndpoint = parsed["deepLEndpoint"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.deepLEndpoint,
            deepLXEndpoint = parsed["deepLXEndpoint"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.deepLXEndpoint,
            deepLXApiKey = parsed["deepLXApiKey"]?.jsonPrimitive?.contentOrNull?.take(500)
                ?: current.deepLXApiKey,
            verifiedSignature = parsed["verifiedSignature"]?.jsonPrimitive?.contentOrNull?.take(120).orEmpty()
        ).sanitized
    }

    private fun writeSubtitleBehaviorPrefs(body: String) {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return
        parsed["preferBilingualSubtitleOnOsd"]?.jsonPrimitive?.booleanOrNull?.let {
            Prefs.preferBilingualSubtitleOnOsd = it
        }
        parsed["preferCustomSecondarySubtitle"]?.jsonPrimitive?.booleanOrNull?.let {
            Prefs.preferCustomSecondarySubtitle = it
        }
    }

    private fun parseSubtitleTranslationTestSentence(body: String): String {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return DefaultSubtitleTranslationTestSentence
        val sentence = parsed["testSentence"]?.jsonPrimitive?.contentOrNull
            ?.take(300)
            ?.trim()
        return sentence.takeUnless { it.isNullOrBlank() } ?: DefaultSubtitleTranslationTestSentence
    }

    private fun subtitleConfigHtml(): String {
        val providerOptions = SubtitleTranslationProviderType.entries.joinToString("\n") {
            """<option value="${it.name}">${it.displayName}</option>"""
        }
        val languageOptions = SubtitleLanguages.supported.joinToString("\n") {
            """<option value="${it.code}">${it.displayName}</option>"""
        }
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>双语字幕配置</title>
              <style>
                :root {
                  --bg: #111214;
                  --panel: #1a1c20;
                  --panel-2: #23262d;
                  --border: rgba(255,255,255,0.1);
                  --muted: #a5adba;
                  --text: #f6f7fb;
                  --accent: #8ee6d1;
                  --danger: #ff9d9d;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  background: var(--bg);
                  color: var(--text);
                  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif;
                }
                .page { max-width: 960px; margin: 0 auto; padding: 24px 18px 48px; }
                .hero { padding: 10px 4px 18px; }
                h1 { margin: 0 0 8px; font-size: 28px; }
                p { margin: 0; color: var(--muted); line-height: 1.5; }
                .card {
                  background: var(--panel);
                  border: 1px solid var(--border);
                  border-radius: 18px;
                  overflow: hidden;
                  box-shadow: 0 18px 50px rgba(0,0,0,0.28);
                }
                .section { padding: 18px 20px; border-top: 1px solid var(--border); }
                .section:first-child { border-top: none; }
                .section-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; }
                .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
                @media (max-width: 720px) { .grid { grid-template-columns: 1fr; } }
                label { display: grid; gap: 7px; color: var(--muted); font-size: 14px; }
                input, textarea, select {
                  width: 100%;
                  border: 1px solid var(--border);
                  border-radius: 12px;
                  background: var(--panel-2);
                  color: var(--text);
                  padding: 12px 13px;
                  font-size: 15px;
                  outline: none;
                }
                input:focus, textarea:focus, select:focus {
                  border-color: var(--accent);
                  box-shadow: 0 0 0 3px rgba(142,230,209,0.14);
                }
                textarea { min-height: 170px; resize: vertical; line-height: 1.5; }
                .switch-row { display: grid; gap: 12px; }
                .switch { display: inline-flex; align-items: center; gap: 10px; color: var(--text); font-size: 15px; }
                .switch input { width: 18px; height: 18px; }
                .switch-note { margin: 6px 0 0 28px; color: var(--muted); font-size: 13px; line-height: 1.45; }
                .hint { color: var(--muted); font-size: 13px; margin-top: 8px; }
                .provider-block { display: none; margin-top: 14px; }
                .provider-block.active { display: block; }
                .action-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
                .buttons { display: flex; gap: 10px; flex-wrap: wrap; }
                button {
                  padding: 12px 20px;
                  border: none;
                  border-radius: 999px;
                  background: var(--accent);
                  color: #081512;
                  font-size: 15px;
                  font-weight: 700;
                  cursor: pointer;
                }
                button.secondary { background: #d1d7e8; }
                button:disabled { cursor: not-allowed; opacity: 0.45; }
                #result { min-height: 22px; color: var(--muted); font-size: 14px; }
                #result.error { color: var(--danger); }
                .test-area { display: grid; gap: 12px; margin-top: 16px; }
                .test-output {
                  min-height: 54px;
                  border: 1px solid var(--border);
                  border-radius: 12px;
                  background: rgba(255,255,255,0.045);
                  padding: 12px 13px;
                  color: var(--text);
                  white-space: pre-wrap;
                  line-height: 1.45;
                }
                .test-output.empty { color: var(--muted); }
              </style>
            </head>
            <body>
              <div class="page">
                <div class="hero">
                  <h1>双语字幕</h1>
                  <p>配置字幕翻译服务。启用前需要先测试连接；修改任一字段后需要重新测试。</p>
                </div>
                <div class="card">
                  <div class="section">
                    <div class="section-title">基础设置</div>
                    <div class="switch-row">
                      <div>
                        <label class="switch"><input id="preferBilingualSubtitleOnOsd" type="checkbox" /> 优先使用双语字幕</label>
                        <div class="switch-note">开启后，底部 OSD 字幕按钮会先恢复主字幕，再按副字幕排序尝试恢复副字幕。</div>
                      </div>
                      <div>
                        <label class="switch"><input id="preferCustomSecondarySubtitle" type="checkbox" /> 优先使用自定义字幕</label>
                        <div class="switch-note">开启后，自定义翻译字幕会排在副字幕列表前面；仍需要当前视频已有主字幕作为翻译源。</div>
                      </div>
                    </div>
                    <div class="grid" style="margin-top: 14px;">
                      <label>翻译服务
                        <select id="providerType">$providerOptions</select>
                      </label>
                      <label>目标语言
                        <select id="targetLanguage">$languageOptions</select>
                      </label>
                      <label>预翻译秒数
                        <input id="preTranslateSeconds" type="number" min="15" max="600" />
                      </label>
                    </div>
                  </div>
                  <div class="section">
                    <div class="section-title">服务凭据</div>
                    <div id="provider-OpenAiCompatible" class="provider-block">
                      <div class="grid">
                        <label>Base URL
                          <input id="openAiBaseUrl" autocomplete="off" placeholder="https://api.openai.com/v1" />
                        </label>
                        <label>模型名
                          <input id="openAiModel" autocomplete="off" placeholder="gpt-4.1-mini" />
                        </label>
                        <label style="grid-column: 1 / -1;">API Key
                          <input id="openAiApiKey" type="password" autocomplete="off" placeholder="sk-..." />
                        </label>
                      </div>
                    </div>
                    <div id="provider-Baidu" class="provider-block">
                      <div class="grid">
                        <label>App ID <input id="baiduAppId" autocomplete="off" /></label>
                        <label>App Key <input id="baiduAppKey" type="password" autocomplete="off" /></label>
                      </div>
                    </div>
                    <div id="provider-Microsoft" class="provider-block">
                      <div class="grid">
                        <label>Key <input id="microsoftKey" type="password" autocomplete="off" /></label>
                        <label>Region <input id="microsoftRegion" autocomplete="off" placeholder="eastasia" /></label>
                        <label style="grid-column: 1 / -1;">Endpoint
                          <input id="microsoftEndpoint" autocomplete="off" placeholder="https://api.cognitive.microsofttranslator.com" />
                        </label>
                      </div>
                    </div>
                    <div id="provider-DeepL" class="provider-block">
                      <div class="grid">
                        <label>API Key <input id="deepLApiKey" type="password" autocomplete="off" /></label>
                        <label>Endpoint <input id="deepLEndpoint" autocomplete="off" placeholder="https://api-free.deepl.com" /></label>
                      </div>
                    </div>
                    <div id="provider-DeepLX" class="provider-block">
                      <div class="grid">
                        <label>Endpoint <input id="deepLXEndpoint" autocomplete="off" placeholder="https://your-deeplx/translate" /></label>
                        <label>API Key（可选）<input id="deepLXApiKey" type="password" autocomplete="off" /></label>
                      </div>
                    </div>
                  </div>
                  <div id="openAiPromptSection" class="section">
                    <div class="section-title">Prompt</div>
                    <textarea id="openAiPrompt"></textarea>
                    <div class="hint">可用占位符：{targetlanguage}、{items}、{context}。</div>
                  </div>
                  <div class="section">
                    <div class="section-title">上下文和批量</div>
                    <div class="grid">
                      <label>上文字幕条数 <input id="contextBefore" type="number" min="0" max="20" /></label>
                      <label>下文字幕条数 <input id="contextAfter" type="number" min="0" max="20" /></label>
                      <label>每次请求字幕条数 <input id="requestBatchSize" type="number" min="1" max="80" /></label>
                    </div>
                    <div class="test-area">
                      <label>测试例句
                        <input id="testSentence" autocomplete="off" value="${DefaultSubtitleTranslationTestSentence}" />
                      </label>
                      <label>测试翻译效果
                        <div id="testOutput" class="test-output empty">点击“测试连接”后，这里会显示服务返回的翻译结果。</div>
                      </label>
                    </div>
                    <div class="action-bar">
                      <div class="buttons">
                        <button class="secondary" onclick="testConfig()">测试连接</button>
                        <button id="saveButton" onclick="saveConfig()" disabled>保存配置</button>
                      </div>
                      <div id="result"></div>
                    </div>
                  </div>
                </div>
              </div>
              <script>
                const el = (id) => document.getElementById(id);
                let verifiedSignature = "";
                let currentSignature = "";
                let resultTimer = 0;
                const preferenceFields = ['preferBilingualSubtitleOnOsd','preferCustomSecondarySubtitle'];
                const serviceFields = [
                  'providerType','targetLanguage',
                  'preTranslateSeconds','openAiBaseUrl','openAiApiKey','openAiModel','openAiPrompt',
                  'baiduAppId','baiduAppKey','microsoftKey','microsoftRegion','microsoftEndpoint',
                  'deepLApiKey','deepLEndpoint','deepLXEndpoint','deepLXApiKey',
                  'contextBefore','contextAfter','requestBatchSize'
                ];
                const clampInt = (value, min, max) => {
                  const parsed = Number.parseInt(value, 10);
                  if (Number.isNaN(parsed)) return min;
                  return Math.min(max, Math.max(min, parsed));
                };
                function payload() {
                  return {
                    preferBilingualSubtitleOnOsd: el('preferBilingualSubtitleOnOsd').checked,
                    preferCustomSecondarySubtitle: el('preferCustomSecondarySubtitle').checked,
                    providerType: el('providerType').value,
                    targetLanguage: el('targetLanguage').value,
                    preTranslateSeconds: clampInt(el('preTranslateSeconds').value, 15, 600),
                    openAiBaseUrl: el('openAiBaseUrl').value,
                    openAiApiKey: el('openAiApiKey').value,
                    openAiModel: el('openAiModel').value,
                    openAiPrompt: el('openAiPrompt').value,
                    baiduAppId: el('baiduAppId').value,
                    baiduAppKey: el('baiduAppKey').value,
                    microsoftKey: el('microsoftKey').value,
                    microsoftRegion: el('microsoftRegion').value,
                    microsoftEndpoint: el('microsoftEndpoint').value,
                    deepLApiKey: el('deepLApiKey').value,
                    deepLEndpoint: el('deepLEndpoint').value,
                    deepLXEndpoint: el('deepLXEndpoint').value,
                    deepLXApiKey: el('deepLXApiKey').value,
                    contextBefore: clampInt(el('contextBefore').value, 0, 20),
                    contextAfter: clampInt(el('contextAfter').value, 0, 20),
                    requestBatchSize: clampInt(el('requestBatchSize').value, 1, 80),
                    verifiedSignature
                  };
                }
                function showProvider() {
                  document.querySelectorAll('.provider-block').forEach(node => node.classList.remove('active'));
                  const providerType = el('providerType').value;
                  const node = el('provider-' + providerType);
                  if (node) node.classList.add('active');
                  el('openAiPromptSection').style.display = providerType === 'OpenAiCompatible' ? 'block' : 'none';
                }
                function showStatus(message, isError = false, autoClear = true) {
                  window.clearTimeout(resultTimer);
                  el('result').className = isError ? 'error' : '';
                  el('result').textContent = message;
                  if (autoClear && message) {
                    resultTimer = window.setTimeout(() => {
                      el('result').textContent = '';
                      el('result').className = '';
                    }, 3000);
                  }
                }
                function setTestOutput(text, isEmpty = false) {
                  el('testOutput').textContent = text;
                  el('testOutput').className = isEmpty ? 'test-output empty' : 'test-output';
                }
                function markDirty() {
                  verifiedSignature = "";
                  el('saveButton').disabled = true;
                  showStatus('配置已修改，请先测试连接');
                  showProvider();
                }
                function markPreferenceDirty() {
                  showStatus('偏好已修改，可以保存');
                  if (currentSignature) el('saveButton').disabled = false;
                }
                async function loadConfig() {
                  const response = await fetch('/api/subtitle/config');
                  const config = await response.json();
                  el('preferBilingualSubtitleOnOsd').checked = Boolean(config.preferBilingualSubtitleOnOsd);
                  el('preferCustomSecondarySubtitle').checked = Boolean(config.preferCustomSecondarySubtitle);
                  el('providerType').value = config.providerType || 'OpenAiCompatible';
                  el('targetLanguage').value = config.targetLanguage || 'en';
                  el('preTranslateSeconds').value = config.preTranslateSeconds ?? 90;
                  el('openAiBaseUrl').value = config.openAiBaseUrl || '';
                  el('openAiApiKey').value = config.openAiApiKey || '';
                  el('openAiModel').value = config.openAiModel || '';
                  el('openAiPrompt').value = config.openAiPrompt || ${DefaultSubtitleTranslationPrompt.quoteJs()};
                  el('baiduAppId').value = config.baiduAppId || '';
                  el('baiduAppKey').value = config.baiduAppKey || '';
                  el('microsoftKey').value = config.microsoftKey || '';
                  el('microsoftRegion').value = config.microsoftRegion || '';
                  el('microsoftEndpoint').value = config.microsoftEndpoint || 'https://api.cognitive.microsofttranslator.com';
                  el('deepLApiKey').value = config.deepLApiKey || '';
                  el('deepLEndpoint').value = config.deepLEndpoint || 'https://api-free.deepl.com';
                  el('deepLXEndpoint').value = config.deepLXEndpoint || '';
                  el('deepLXApiKey').value = config.deepLXApiKey || '';
                  el('contextBefore').value = config.contextBefore ?? 2;
                  el('contextAfter').value = config.contextAfter ?? 1;
                  el('requestBatchSize').value = config.requestBatchSize ?? 12;
                  verifiedSignature = config.verifiedSignature || '';
                  currentSignature = config.currentSignature || '';
                  showProvider();
                  el('saveButton').disabled = !config.verified;
                  showStatus(config.verified ? '当前配置已通过测试' : '启用前请先测试连接');
                  preferenceFields.forEach(id => el(id).addEventListener('input', markPreferenceDirty));
                  preferenceFields.forEach(id => el(id).addEventListener('change', markPreferenceDirty));
                  serviceFields.forEach(id => el(id).addEventListener('input', markDirty));
                  serviceFields.forEach(id => el(id).addEventListener('change', markDirty));
                }
                async function testConfig() {
                  showStatus('正在测试连接...', false, false);
                  el('saveButton').disabled = true;
                  setTestOutput('正在请求翻译服务...', true);
                  const testPayload = payload();
                  testPayload.testSentence = el('testSentence').value;
                  const response = await fetch('/api/subtitle/test', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(testPayload)
                  });
                  const data = await response.json().catch(() => ({}));
                  if (response.ok && data.success) {
                    verifiedSignature = data.signature || '';
                    el('saveButton').disabled = false;
                    setTestOutput(data.translation || '服务返回为空');
                    showStatus('测试通过，可以保存配置');
                  } else {
                    verifiedSignature = '';
                    setTestOutput('测试失败，未取得翻译结果。', true);
                    showStatus(data.error || '测试失败', true);
                  }
                }
                async function saveConfig() {
                  const response = await fetch('/api/subtitle/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload())
                  });
                  const data = await response.json().catch(() => ({}));
                  showStatus(response.ok ? '配置已保存' : (data.error || '保存失败，请重试'), !response.ok);
                }
                loadConfig().catch(() => {
                  showStatus('读取配置失败，请确认电视端服务正常运行', true);
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private data class LogItem(
        val name: String,
        val size: Long,
        val lastModified: Long,
        val type: String
    )

    private fun itemsToJson(items: List<LogItem>): String {
        return buildString {
            append('[')
            for (i in items.indices) {
                val it = items[i]
                if (i != 0) append(',')
                append('{')
                append("\"name\":\"").append(jsonEscape(it.name)).append("\",")
                append("\"size\":").append(it.size).append(',')
                append("\"lastModified\":").append(it.lastModified).append(',')
                append("\"type\":\"").append(jsonEscape(it.type)).append('"')
                append('}')
            }
            append(']')
        }
    }

    private fun parseFormBody(body: String): Parameters {
        return Parameters.build {
            body.split('&')
                .filter { it.isNotBlank() }
                .forEach { pair ->
                    val key = pair.substringBefore('=').decodeUrlFormPart()
                    val value = pair.substringAfter('=', "").decodeUrlFormPart()
                    append(key, value)
                }
        }
    }

    private fun String.decodeUrlFormPart(): String {
        return java.net.URLDecoder.decode(this, Charsets.UTF_8.name())
    }

    private fun String.quoteJs(): String = "\"${jsonEscape(this)}\""

    private fun searchInputHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>NeoBV 搜索输入</title>
              <style>
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  min-height: 100vh;
                  background: #111214;
                  color: #f6f7fb;
                  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif;
                  display: grid;
                  place-items: center;
                  padding: 24px;
                }
                main {
                  width: min(520px, 100%);
                }
                h1 {
                  margin: 0 0 10px;
                  font-size: 28px;
                }
                p {
                  margin: 0 0 18px;
                  color: #a5adba;
                  line-height: 1.5;
                }
                form {
                  display: grid;
                  gap: 12px;
                }
                textarea {
                  width: 100%;
                  min-height: 140px;
                  resize: vertical;
                  border: 1px solid rgba(255,255,255,0.12);
                  border-radius: 14px;
                  background: #1c1f26;
                  color: #fff;
                  padding: 14px 16px;
                  font-size: 18px;
                  line-height: 1.5;
                  outline: none;
                }
                textarea:focus {
                  border-color: #72d58a;
                  box-shadow: 0 0 0 3px rgba(114,213,138,0.16);
                }
                button {
                  height: 48px;
                  border: none;
                  border-radius: 14px;
                  background: #72d58a;
                  color: #0d1510;
                  font-size: 17px;
                  font-weight: 700;
                }
                .status {
                  min-height: 24px;
                  color: #a5adba;
                  font-size: 15px;
                }
              </style>
            </head>
            <body>
              <main>
                <h1>手机输入搜索</h1>
                <p>输入文字后直接在电视上搜索。</p>
                <form id="form">
                  <textarea id="keyword" name="keyword" autofocus placeholder="输入 BV 号、关键词或链接"></textarea>
                  <button type="submit">搜索</button>
                  <div id="status" class="status"></div>
                </form>
              </main>
              <script>
                const form = document.getElementById('form');
                const input = document.getElementById('keyword');
                const status = document.getElementById('status');
                let statusTimer = null;
                const setStatus = (text) => {
                  status.textContent = text;
                  if (statusTimer) clearTimeout(statusTimer);
                  if (text) statusTimer = setTimeout(() => { status.textContent = ''; }, 3000);
                };
                form.addEventListener('submit', async (event) => {
                  event.preventDefault();
                  setStatus('正在搜索...');
                  const body = new URLSearchParams();
                  body.set('keyword', input.value);
                  const response = await fetch('/api/search/input', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body
                  });
                  setStatus(response.ok ? '已搜索' : '搜索失败，请检查电视和手机是否在同一网络');
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun String.toRuleLinesForServer(): List<String> {
        return lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun jsonEscape(s: String): String {
        return buildString(s.length + 16) {
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")   // 匹配单反斜杠，追加 "\" 和 "\"
                    '"'  -> append("\\\"")   // 匹配双引号，追加 "\" 和 """
                    '\n' -> append("\\n")    // 匹配换行，追加 "\" 和 "n"
                    '\r' -> append("\\r")    // 匹配回车，追加 "\" 和 "r"
                    '\t' -> append("\\t")    // 匹配制表符，追加 "\" 和 "t"
                    '\b' -> append("\\b")    // 匹配退格符，追加 "\" 和 "b"
                    '\u000C' -> append("\\f") // 匹配换页符
                    else -> append(ch)
                }
            }
        }
    }

    private fun readAssetBytesOrNull(assetPath: String): ByteArray? {
        return runCatching {
            BVApp.context.assets.open(assetPath).use { it.readBytes() }
        }.recoverCatching { e ->
            // assets.open 不存在通常会抛 FileNotFoundException
            if (e is FileNotFoundException) null else throw e
        }.getOrNull()
    }

    private fun contentTypeFor(path: String): ContentType {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> ContentType.Text.Html.withCharset(Charsets.UTF_8)
            "css" -> ContentType.Text.CSS.withCharset(Charsets.UTF_8)
            "js" -> ContentType.Application.JavaScript.withCharset(Charsets.UTF_8)
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "svg" -> ContentType.Image.SVG
            "ico" -> ContentType.Image.XIcon
            else -> ContentType.Application.OctetStream
        }
    }

    private fun parseSponsorBlockConfig(body: String): SponsorBlockConfig? {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val enabled = parsed["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
        val policies = parsed["categoryPolicy"]?.jsonObject.orEmpty()
        return SponsorBlockConfig(
            enabled = enabled,
            categoryPolicy = SponsorBlockConfig.supportedCategories.associateWith { category ->
                policies[category]?.jsonPrimitive?.contentOrNull
                    ?.let { runCatching { enumValueOf<SkipPolicy>(it) }.getOrNull() }
                    ?: SkipPolicy.Disabled
            }
        )
    }

    private fun sponsorBlockCategoryDisplayName(category: String): String = when (category) {
        "sponsor" -> "赞助/恰饭"
        "selfpromo" -> "无偿/自我推广"
        "exclusive_access" -> "独家访问/抢先体验"
        "interaction" -> "三连/互动提醒"
        "poi_highlight" -> "精彩时刻/重点"
        "intro" -> "过场/开场动画"
        "outro" -> "鸣谢/结束画面"
        "preview" -> "回顾/概要"
        "filler" -> "填充内容/前黑/后黑"
        "music_offtopic" -> "音乐-非音乐部分"
        else -> category
    }

    private fun sponsorBlockCategoryDescription(category: String): String = when (category) {
        "sponsor" -> "付费推广、推荐和直接广告。"
        "selfpromo" -> "作者自己的商品、活动、自我宣传内容。"
        "exclusive_access" -> "仅对独家视频或抢先体验内容打标。"
        "interaction" -> "视频中间提醒观众一键三连或参与互动。"
        "poi_highlight" -> "常见于“精彩片段”或重点时刻。"
        "intro" -> "没有实际内容的过场片段，例如开场动画。"
        "outro" -> "鸣谢画面或片尾画面，不含主体内容。"
        "preview" -> "前情回顾、后续预告或概要片段。"
        "filler" -> "与视频主体没有实质关联的填充内容。"
        "music_offtopic" -> "音乐视频里与音乐本体无关的非音乐部分。"
        else -> ""
    }

    private fun getLocalIpv4Address(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        }.getOrNull() ?: "127.0.0.1"
    }
}
