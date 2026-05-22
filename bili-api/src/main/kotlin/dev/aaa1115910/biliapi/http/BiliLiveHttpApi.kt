package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.live.DanmuInfoData
import dev.aaa1115910.biliapi.http.entity.live.HistoryDanmaku
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
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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
    suspend fun getLiveDanmuInfo(roomId: Int): BiliResponse<DanmuInfoData> =
        client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
            parameter("type", 0)
            parameter("web_location", "444.8")
            header("Referer", "https://live.bilibili.com/")
            header("Origin", "https://live.bilibili.com")
        }.body()

    /**
     * 获取直播间[roomId]的信息
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): BiliResponse<RoomPlayInfoData> =
        client.get("/xlive/web-room/v1/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
        }.body()

    /**
     * 获取直播间[roomId]的历史弹幕
     */
    suspend fun getLiveDanmuHistory(roomId: Int): BiliResponse<HistoryDanmaku> =
        client.get("/xlive/web-room/v1/dM/gethistory") {
            parameter("roomid", roomId)
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
        parameter("codec", "0,1")
        parameter("qn", qn)
        parameter("platform", "web")
        parameter("ptype", 8)
        parameter("dolby", 5)
        parameter("panorama", 1)
        if (sessData.isNotBlank()) {
            header("Cookie", "SESSDATA=$sessData;")
        }
    }.body()

}
