package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.live.DanmuInfoData
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoData
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.encApiSign
import dev.aaa1115910.biliapi.http.util.injectBuvid3Cookie
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Base64

object BiliLiveHttpApi {
    private var endPoint: String = ""
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    host = "api.live.bilibili.com"
                    protocol = URLProtocol.HTTPS
                }
            }
        }.apply {
            encApiSign()
            injectBuvid3Cookie()
        }
    }

    /**
     * 获取直播间[roomId]的弹幕连接地址等信息，例如 token
     */
    suspend fun getLiveDanmuInfo(
        roomId: Int,
        uid: Long = 0L,
        sessData: String = "",
        biliJct: String = "",
        uidCkMd5: String = "",
        sid: String = "",
        buvid3: String = BiliHttpApi.buvid3
    ): BiliResponse<DanmuInfoData> =
        client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
            parameter("type", 0)
            parameter("web_location", "444.8")
            header("Referer", "https://live.bilibili.com/")
            header("Origin", "https://live.bilibili.com")
            buildLiveDanmakuCookieHeader(
                uid = uid,
                sessData = sessData,
                biliJct = biliJct,
                uidCkMd5 = uidCkMd5,
                sid = sid,
                buvid3 = buvid3
            ).takeIf { it.isNotBlank() }?.let { header("Cookie", it) }
        }.body()

    private fun buildLiveDanmakuCookieHeader(
        uid: Long = 0L,
        sessData: String = "",
        biliJct: String = "",
        uidCkMd5: String = "",
        sid: String = "",
        buvid3: String = ""
    ): String =
        buildList {
            if (uid > 0L) add("DedeUserID=$uid")
            if (uidCkMd5.isNotBlank()) add("DedeUserID__ckMd5=$uidCkMd5")
            if (sessData.isNotBlank()) add("SESSDATA=$sessData")
            if (biliJct.isNotBlank()) add("bili_jct=$biliJct")
            if (sid.isNotBlank()) add("sid=$sid")
            if (buvid3.isNotBlank()) add("buvid3=$buvid3")
        }.joinToString("; ")

    /**
     * 获取直播间[roomId]的信息
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): BiliResponse<RoomPlayInfoData> =
        client.get("/xlive/web-room/v1/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
        }.body()

    suspend fun getRecommendedLives(
        page: Int = 1,
        pageSize: Int = 30,
        parentId: Int = 0,
        areaId: Int = 0
    ): BiliResponse<JsonElement> {
        return if (parentId > 0) {
            client.get("/room/v3/area/getRoomList") {
                parameter("parent_area_id", parentId)
                parameter("area_id", areaId)
                parameter("sort_type", "online")
                parameter("page", page)
                parameter("page_size", pageSize)
            }.body()
        } else {
            client.get("/room/v1/Area/getListByAreaID") {
                parameter("areaId", areaId)
                parameter("sort", "online")
                parameter("pageSize", pageSize)
                parameter("page", page)
                parameter("parent_area_id", parentId)
            }.body()
        }
    }

    suspend fun getFollowedLives(
        page: Int = 1,
        pageSize: Int = 20,
        sessData: String = ""
    ): BiliResponse<JsonObject> = client.get("/relation/v1/feed/feed_list") {
        parameter("page", page)
        parameter("pagesize", pageSize)
        if (sessData.isNotBlank()) {
            header("Cookie", "SESSDATA=$sessData;")
        }
    }.body()

    suspend fun getParentAreas(): BiliResponse<JsonArray> =
        client.get("/room/v1/Area/getList") {
            parameter("need_entrance", 1)
            parameter("parent_area_id", 0)
        }.body()

    suspend fun getLiveRoomInfo(roomId: Int): BiliResponse<JsonObject> =
        client.get("/room/v1/Room/get_info") {
            parameter("room_id", roomId)
        }.body()

    suspend fun getLiveRoomWebInfo(roomId: Int): BiliResponse<JsonObject> =
        client.get("/xlive/web-room/v1/index/getInfoByRoom") {
            parameter("room_id", roomId)
        }.body()

    suspend fun getLivePlayUrl(
        roomId: Int,
        qn: Int = 10000,
        sessData: String = ""
    ): BiliResponse<JsonObject> = client.get("/xlive/web-room/v2/index/getRoomPlayInfo") {
        parameter("room_id", roomId)
        parameter("protocol", "0,1")
        parameter("format", "0,1,2")
        parameter("codec", "0,1,2")
        parameter("qn", qn)
        parameter("platform", "web")
        parameter("ptype", 8)
        parameter("dolby", 5)
        parameter("panorama", 1)
        if (sessData.isNotBlank()) {
            header("Cookie", "SESSDATA=$sessData;")
        }
    }.body()

    suspend fun sendLiveRoomEntryAction(
        roomId: Int,
        sessData: String = "",
        csrf: String = "",
        buvid3: String = ""
    ): String = client.post("/xlive/web-room/v1/index/roomEntryAction") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("room_id", "$roomId")
                    append("roomid", "$roomId")
                    append("platform", "pc")
                    csrf.takeIf { it.isNotBlank() }?.let {
                        append("csrf", it)
                        append("csrf_token", it)
                    }
                }
            )
        )
        header("Referer", "https://live.bilibili.com/$roomId")
        header("Origin", "https://live.bilibili.com")
        buildLiveCookie(sessData, csrf, buvid3).takeIf { it.isNotBlank() }?.let { header("Cookie", it) }
    }.bodyAsText()

    suspend fun sendLiveUserOnlineHeart(
        roomId: Int,
        sessData: String = "",
        csrf: String = "",
        buvid3: String = ""
    ): String = client.post("/User/userOnlineHeart") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("room_id", "$roomId")
                    append("roomid", "$roomId")
                    csrf.takeIf { it.isNotBlank() }?.let {
                        append("csrf", it)
                        append("csrf_token", it)
                    }
                }
            )
        )
        header("Referer", "https://live.bilibili.com/$roomId")
        header("Origin", "https://live.bilibili.com")
        buildLiveCookie(sessData, csrf, buvid3).takeIf { it.isNotBlank() }?.let { header("Cookie", it) }
    }.bodyAsText()

    suspend fun sendLiveWebHeartbeat(
        roomId: Int,
        intervalSeconds: Int = 60,
        sessData: String = "",
        csrf: String = "",
        buvid3: String = ""
    ): String {
        val heartbeat = "$intervalSeconds|$roomId|1|0"
        val hb = Base64.getEncoder().encodeToString(heartbeat.toByteArray(Charsets.UTF_8))
        return client.get("https://live-trace.bilibili.com/xlive/rdata-interface/v1/heartbeat/webHeartBeat") {
            parameter("hb", hb)
            parameter("pf", "web")
            header("Referer", "https://live.bilibili.com/$roomId")
            header("Origin", "https://live.bilibili.com")
            buildLiveCookie(sessData, csrf, buvid3).takeIf { it.isNotBlank() }?.let { header("Cookie", it) }
        }.bodyAsText()
    }

    suspend fun getLiveMasterPlaylist(
        roomId: Int,
        mid: Long,
        qn: Int = 10000,
        sessData: String = ""
    ): String = client.get("/xlive/play-gateway/master/url") {
        parameter("cid", roomId)
        parameter("mid", mid)
        parameter("qn", qn)
        parameter("pt", "web")
        parameter("p2p_type", -1)
        parameter("net", 0)
        parameter("free_type", 0)
        parameter("build", 0)
        parameter("feature", 2)
        parameter("drm_type", 0)
        parameter("cam_id", 0)
        header("Referer", "https://live.bilibili.com/$roomId")
        header("Origin", "https://live.bilibili.com")
        if (sessData.isNotBlank()) {
            header("Cookie", "SESSDATA=$sessData;")
        }
    }.bodyAsText()

}

private fun buildLiveCookie(
    sessData: String,
    csrf: String,
    buvid3: String
): String = buildList {
    sessData.takeIf { it.isNotBlank() }?.let { add("SESSDATA=$it") }
    csrf.takeIf { it.isNotBlank() }?.let { add("bili_jct=$it") }
    buvid3.takeIf { it.isNotBlank() }?.let { add("buvid3=$it") }
}.joinToString("; ")
