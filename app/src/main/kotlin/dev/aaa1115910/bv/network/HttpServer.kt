package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.plugin.core.PluginManager
import dev.aaa1115910.bv.plugin.impl.sponsorblock.PrefsSponsorBlockConfigStore
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SkipPolicy
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SponsorBlockConfig
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SponsorBlockPlugin
import dev.aaa1115910.bv.util.LogCatcherUtil
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object HttpServer {
    private const val SERVER_PORT = 2944
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var currentMpdContent: String? = null

    fun startServer() {
        if (server != null) return
        server = embeddedServer(CIO, port = SERVER_PORT) {
            homeModule()
            logsUiStaticModule()
            logsApiModule()
            sponsorBlockModule()
        }
        server?.start(wait = false)
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
                val config = runBlocking { store.readConfig() }
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
                    store.writeConfig(newConfig)
                    store.setEnabled(newConfig.enabled)
                }
                call.respondText(
                    text = """{"success":true}""",
                    contentType = ContentType.Application.Json
                )
            }

            get("/sponsorblock") {
                val store = PrefsSponsorBlockConfigStore()
                val config = runBlocking { store.readConfig() }
                val checkedEnabled = if (config.enabled) "checked" else ""
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
                            .dot-sponsor { background: #43d676; }
                            .dot-selfpromo { background: #ffe24d; }
                            .dot-exclusive_access { background: #1fd2a4; }
                            .dot-interaction { background: #d94bff; }
                            .dot-poi_highlight { background: #ff4aa5; }
                            .dot-intro { background: #22d6ff; }
                            .dot-outro { background: #1547ff; }
                            .dot-preview { background: #33a3ff; }
                            .dot-filler { background: #8f96a3; }
                            .dot-music_offtopic { background: #ffab1f; }
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
                                  <label class="switch"><input id="enabled" type="checkbox" $checkedEnabled /> 启用空降助手</label>
                                  <span style="color: var(--muted);">服务器状态：正常</span>
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
                                enabled: document.getElementById('enabled').checked,
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
