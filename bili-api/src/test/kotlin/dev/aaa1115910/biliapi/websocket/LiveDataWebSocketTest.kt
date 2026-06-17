package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.HostListItem
import dev.aaa1115910.biliapi.http.entity.live.SuperChatEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.EOFException
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LiveDataWebSocketTest {
    @Test
    fun `live danmaku host prefers broadcast host`() {
        val hosts = listOf(
            HostListItem(host = "backup.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244),
            HostListItem(host = "broadcastlv.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244),
            HostListItem(host = "another.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244)
        )

        assertEquals(
            "broadcastlv.chat.bilibili.com",
            LiveDataWebSocket.preferLiveDanmakuHosts(hosts).first().host
        )
    }

    @Test
    fun `live danmaku endpoints include wss and ws variants`() {
        val endpoints = LiveDataWebSocket.buildLiveDanmakuEndpoints(
            listOf(
                HostListItem(host = "backup.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244),
                HostListItem(host = "broadcastlv.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244)
            )
        )

        assertEquals("wss://broadcastlv.chat.bilibili.com:443", endpoints[0].displayName())
        assertEquals("ws://broadcastlv.chat.bilibili.com:2244", endpoints[1].displayName())
        assertTrue(endpoints.any { it.displayName() == "wss://backup.chat.bilibili.com:443" })
        assertTrue(endpoints.any { it.displayName() == "ws://backup.chat.bilibili.com:2244" })
    }

    @Test
    fun `live auth packet requests protocol version 2 with uid`() {
        val packet = LiveDataWebSocket.buildLiveAuthPacket(
            roomId = 1234,
            uid = 5678L,
            token = "token"
        )
        val header = ByteBuffer.wrap(packet)

        assertEquals(packet.size, header.getInt(0))
        assertEquals(16, header.getShort(4).toInt())
        assertEquals(1, header.getShort(6).toInt())
        assertEquals(7, header.getInt(8))

        val body = Json.parseToJsonElement(packet.copyOfRange(16, packet.size).decodeToString()).jsonObject
        assertEquals("5678", body["uid"]?.jsonPrimitive?.content)
        assertEquals("1234", body["roomid"]?.jsonPrimitive?.content)
        assertEquals("2", body["protover"]?.jsonPrimitive?.content)
        assertEquals("token", body["key"]?.jsonPrimitive?.content)
    }

    @Test
    fun `live auth uid falls back to guest when session cookie is unavailable`() {
        assertEquals(0L, LiveDataWebSocket.normalizeLiveDanmakuUid(uid = 5678L, sessData = ""))
        assertEquals(5678L, LiveDataWebSocket.normalizeLiveDanmakuUid(uid = 5678L, sessData = "sess"))
        assertEquals(0L, LiveDataWebSocket.normalizeLiveDanmakuUid(uid = 0L, sessData = "sess"))
    }

    @Test
    fun `live heartbeat packet uses object body`() {
        val packet = LiveDataWebSocket.buildLiveHeartbeatPacket(sequence = 9)
        val header = ByteBuffer.wrap(packet)

        assertEquals(31, packet.size)
        assertEquals(31, header.getInt(0))
        assertEquals(16, header.getShort(4).toInt())
        assertEquals(1, header.getShort(6).toInt())
        assertEquals(2, header.getInt(8))
        assertEquals(9, header.getInt(12))
        assertEquals("[object Object]", packet.copyOfRange(16, packet.size).decodeToString())
    }

    @Test
    fun `live cookie header includes available web auth cookies`() {
        val header = LiveDataWebSocket.buildLiveCookieHeader(
            uid = 123L,
            sessData = "sess",
            biliJct = "csrf",
            uidCkMd5 = "md5",
            sid = "sid",
            buvid3 = "buvid"
        )

        assertTrue(header.contains("DedeUserID=123"))
        assertTrue(header.contains("DedeUserID__ckMd5=md5"))
        assertTrue(header.contains("SESSDATA=sess"))
        assertTrue(header.contains("bili_jct=csrf"))
        assertTrue(header.contains("sid=sid"))
        assertTrue(header.contains("buvid3=buvid"))
    }

    @Test
    fun `live auth reply packet is parsed`() {
        val packet = livePacket(op = 8, version = 1, body = """{"code":0}""".toByteArray())

        assertTrue(LiveDataWebSocket.isLiveAuthReplySuccess(packet))
    }

    @Test
    fun `live websocket failure reason includes connection stage and endpoint details`() {
        val reason = LiveDataWebSocket.buildLiveWebSocketFailureReason(
            throwable = EOFException("stream closed"),
            endpoint = LiveDataWebSocket.LiveDanmakuEndpoint(
                scheme = "wss",
                host = "broadcastlv.chat.bilibili.com",
                port = 443
            ),
            stage = "connecting",
            endpointIndex = 0,
            endpointCount = 4,
            reconnectAttempt = 2,
            hasCookie = true,
            tokenLength = 12
        )

        assertTrue(reason.contains("EOFException: stream closed"))
        assertTrue(reason.contains("stage=connecting"))
        assertTrue(reason.contains("endpoint=wss://broadcastlv.chat.bilibili.com:443"))
        assertTrue(reason.contains("url=wss://broadcastlv.chat.bilibili.com/sub"))
        assertTrue(reason.contains("idx=1/4"))
        assertTrue(reason.contains("retry=2"))
        assertTrue(reason.contains("cookie=yes"))
        assertTrue(reason.contains("token=12"))
    }

    @Test
    fun `live event parser handles brotli compressed danmaku packet`() = runBlocking {
        val packet = byteArrayOf(
            0x00, 0x00, 0x00, 0xbb.toByte(), 0x00, 0x10, 0x00, 0x03,
            0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01,
            0x1b, 0xc2.toByte(), 0x00, 0x00, 0xac.toByte(), 0x0e, 0x78, 0xc3.toByte(),
            0xe8.toByte(), 0x93.toByte(), 0x0b, 0xbb.toByte(), 0xcc.toByte(), 0x1d, 0x98.toByte(), 0x60,
            0x22, 0x3c, 0xa8.toByte(), 0xfc.toByte(), 0xda.toByte(), 0x4e, 0x83.toByte(), 0xe7.toByte(),
            0xff.toByte(), 0x6f, 0x36, 0x3d, 0xe0.toByte(), 0xcb.toByte(), 0x7f, 0xcc.toByte(),
            0x16, 0x27, 0xf6.toByte(), 0xce.toByte(), 0xa7.toByte(), 0x12, 0xeb.toByte(), 0xe4.toByte(),
            0x14, 0xe9.toByte(), 0xd2.toByte(), 0x1b, 0x3a, 0xfe.toByte(), 0x57, 0x39,
            0xa4.toByte(), 0xe4.toByte(), 0x73, 0x7c, 0x73, 0x8a.toByte(), 0x68, 0x91.toByte(),
            0xa6.toByte(), 0xa3.toByte(), 0xaa.toByte(), 0xc9.toByte(), 0xd2.toByte(), 0xd6.toByte(), 0x57, 0x4e,
            0x5f, 0x15, 0x34, 0xa5.toByte(), 0x93.toByte(), 0x03, 0x3d, 0x28,
            0x1c, 0xa3.toByte(), 0x40, 0x92.toByte(), 0x2c, 0x8b.toByte(), 0x2c, 0xb5.toByte(),
            0x20, 0x6a, 0x59, 0x6e, 0x63, 0x8a.toByte(), 0x08, 0xbe.toByte(),
            0x46, 0xad.toByte(), 0x4e, 0xe5.toByte(), 0xaa.toByte(), 0xaa.toByte(), 0x6e, 0x1a,
            0xaa.toByte(), 0xa0.toByte(), 0xaf.toByte(), 0x76, 0xf5.toByte(), 0x69, 0xef.toByte(), 0x60,
            0x65, 0xa3.toByte(), 0xef.toByte(), 0x60, 0xdf.toByte(), 0xc6.toByte(), 0x2a, 0xe2.toByte(),
            0x94.toByte(), 0x57, 0x21, 0x2b, 0x3f, 0x4e, 0x88.toByte(), 0xe0.toByte(),
            0xb3.toByte(), 0x72, 0x2e, 0xd0.toByte(), 0x38, 0xe7.toByte(), 0x45, 0x1d,
            0x9c.toByte(), 0x72, 0x05, 0x6f, 0xe5.toByte(), 0x17, 0xfd.toByte(), 0xfe.toByte(),
            0xbe.toByte(), 0xeb.toByte(), 0x5f, 0x6d, 0x80.toByte(), 0xf8.toByte(), 0x10, 0x53,
            0x1e, 0xa7.toByte(), 0x79, 0x51, 0xcc.toByte(), 0x79, 0x18, 0x06,
            0xa7.toByte(), 0x1f, 0x14, 0x25, 0x5e, 0xf1.toByte(), 0xb3.toByte(), 0xbc.toByte(),
            0x50, 0xf0.toByte(), 0xcc.toByte(), 0x7d, 0x06, 0xf8.toByte(), 0x42, 0x2c,
            0x8e.toByte(), 0x5a, 0x06
        )

        val event = assertIs<DanmakuEvent>(LiveDataWebSocket.handleLiveEventData(packet).single())
        assertEquals("实时弹幕测试", event.content)
        assertEquals(123456789L, event.mid)
        assertEquals("测试用户", event.username)
    }

    @Test
    fun `live event parser accepts danmaku command suffixes`() = runBlocking {
        val packet = liveCommandPacket(
            """
            {
              "cmd": "DANMU_MSG:4:0:2:2:2:0",
              "info": [
                [0, 1, 25, 16777215],
                "带后缀的实时弹幕",
                [987654321, "后缀用户"]
              ]
            }
            """.trimIndent()
        )

        val event = assertIs<DanmakuEvent>(LiveDataWebSocket.handleLiveEventData(packet).single())
        assertEquals("带后缀的实时弹幕", event.content)
        assertEquals(987654321L, event.mid)
        assertEquals("后缀用户", event.username)
        assertEquals(16777215, event.color)
        assertEquals(1, event.mode)
    }

    @Test
    fun `live event parser reads concatenated uncompressed command packets`() = runBlocking {
        val first = liveCommandPacket(
            """
            {
              "cmd": "DANMU_MSG",
              "info": [
                [0, 1, 25, 16711680],
                "第一条实时弹幕",
                [111, "用户一"]
              ],
              "send_time": 1700000001
            }
            """.trimIndent()
        )
        val second = liveCommandPacket(
            """
            {
              "cmd": "DANMU_MSG",
              "info": [
                [0, 1, 25, 65280],
                "第二条实时弹幕",
                [222, "用户二"]
              ],
              "send_time": 1700000002
            }
            """.trimIndent()
        )

        val events = LiveDataWebSocket.handleLiveEventData(first + second)

        assertEquals(2, events.size)
        assertEquals("第一条实时弹幕", assertIs<DanmakuEvent>(events[0]).content)
        assertEquals(16711680, assertIs<DanmakuEvent>(events[0]).color)
        assertEquals(1_700_000_001_000L, assertIs<DanmakuEvent>(events[0]).eventTimeMs)
        assertEquals("第二条实时弹幕", assertIs<DanmakuEvent>(events[1]).content)
        assertEquals(65280, assertIs<DanmakuEvent>(events[1]).color)
        assertEquals(1_700_000_002_000L, assertIs<DanmakuEvent>(events[1]).eventTimeMs)
    }

    @Test
    fun `live event parser reads app style danmaku extra user fields`() = runBlocking {
        val packet = liveCommandPacket(
            """
            {
              "cmd": "DANMU_MSG",
              "info": [
                [
                  0, 1, 25, 16777215, 1700000003, 0, 0, "", 0, 0, 0, "", 0, {},
                  {},
                  {
                    "extra": "{\"color\":65280,\"mode\":1,\"id_str\":\"abc\"}",
                    "user": {
                      "uid": 333,
                      "base": { "name": "新格式用户" },
                      "medal": { "name": "粉丝牌", "level": 12 }
                    }
                  }
                ],
                "新格式实时弹幕",
                [0, ""]
              ]
            }
            """.trimIndent()
        )

        val event = assertIs<DanmakuEvent>(LiveDataWebSocket.handleLiveEventData(packet).single())

        assertEquals("新格式实时弹幕", event.content)
        assertEquals(333L, event.mid)
        assertEquals("新格式用户", event.username)
        assertEquals("粉丝牌", event.medalName)
        assertEquals(12, event.medalLevel)
        assertEquals(65280, event.color)
        assertEquals(1, event.mode)
    }

    @Test
    fun `live event parser keeps super chat events`() = runBlocking {
        val packet = liveCommandPacket(
            """
            {
              "cmd": "SUPER_CHAT_MESSAGE",
              "data": {
                "id": 123,
                "uid": 456,
                "price": 30,
                "message": "醒目留言",
                "ts": 1700000004,
                "user_info": { "uname": "SC用户" }
              }
            }
            """.trimIndent()
        )

        val event = assertIs<SuperChatEvent>(LiveDataWebSocket.handleLiveEventData(packet).single())

        assertEquals(123L, event.id)
        assertEquals(456L, event.uid)
        assertEquals("SC用户", event.username)
        assertEquals("醒目留言", event.message)
        assertEquals(30L, event.price)
        assertEquals(1_700_000_004_000L, event.eventTimeMs)
    }

    @Test
    fun `live event parser reads emoticon danmaku`() = runBlocking {
        val packet = liveCommandPacket(
            """
            {
              "cmd": "DANMU_MSG",
              "info": [
                [
                  0, 1, 25, 16777215, 1700000003, 0, 0, "", 0, 0, 0, "", 1,
                  {
                    "url": "http://i0.hdslb.com/bfs/live/test_emoticon.png",
                    "gif_url": "http://i0.hdslb.com/bfs/live/test_emoticon.gif",
                    "emoticon_unique": "emoji_test",
                    "width": 60,
                    "height": 60
                  }
                ],
                "表情弹幕内容",
                [111, "表情用户"]
              ]
            }
            """.trimIndent()
        )

        val event = assertIs<DanmakuEvent>(LiveDataWebSocket.handleLiveEventData(packet).single())

        assertEquals("表情弹幕内容", event.content)
        assertEquals(111L, event.mid)
        assertEquals("表情用户", event.username)
        assertEquals("https://i0.hdslb.com/bfs/live/test_emoticon.gif", event.emoticonUrl)
    }

    private fun liveCommandPacket(json: String): ByteArray {
        return livePacket(op = 5, version = 0, body = json.toByteArray())
    }

    private fun livePacket(op: Int, version: Int, body: ByteArray): ByteArray {
        return ByteBuffer.allocate(16 + body.size)
            .putInt(16 + body.size)
            .putShort(16)
            .putShort(version.toShort())
            .putInt(op)
            .putInt(1)
            .put(body)
            .array()
    }
}
