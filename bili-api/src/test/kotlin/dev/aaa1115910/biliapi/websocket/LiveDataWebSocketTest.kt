package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.HostListItem
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LiveDataWebSocketTest {
    @Test
    fun `live danmaku host prefers first usable websocket host`() {
        val hosts = listOf(
            HostListItem(host = "broadcastlv.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244),
            HostListItem(host = "backup.chat.bilibili.com", port = 2243, wssPort = 443, wsPort = 2244)
        )

        assertEquals(
            "broadcastlv.chat.bilibili.com",
            LiveDataWebSocket.chooseLiveDanmakuHost(hosts).host
        )
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
    }

    private fun liveCommandPacket(json: String): ByteArray {
        val body = json.toByteArray()
        return ByteBuffer.allocate(16 + body.size)
            .putInt(16 + body.size)
            .putShort(16)
            .putShort(0)
            .putInt(5)
            .putInt(1)
            .put(body)
            .array()
    }
}
