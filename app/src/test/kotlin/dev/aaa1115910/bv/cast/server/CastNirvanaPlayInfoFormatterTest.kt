package dev.aaa1115910.bv.cast.server

import dev.aaa1115910.bv.cast.CastPlaybackSnapshot
import dev.aaa1115910.bv.cast.CastTransportState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CastNirvanaPlayInfoFormatterTest {
    @Test
    fun `formats GetPlayInfo duration in milliseconds and position in seconds for phone remote`() {
        val playInfo = Json.parseToJsonElement(
            CastNirvanaPlayInfoFormatter.format(
                CastPlaybackSnapshot(
                    state = CastTransportState.PLAYING,
                    positionMs = 4_103L,
                    durationMs = 365_640_000L
                )
            )
        ).jsonObject

        assertEquals("4", playInfo.getValue("position").jsonPrimitive.content)
        assertEquals("365640000", playInfo.getValue("duration").jsonPrimitive.content)
        assertEquals("4", playInfo.getValue("playerStatus").jsonPrimitive.content)
    }

    @Test
    fun `does not let milliseconds make phone seek to hours ahead`() {
        val playInfo = Json.parseToJsonElement(
            CastNirvanaPlayInfoFormatter.format(
                CastPlaybackSnapshot(
                    state = CastTransportState.PLAYING,
                    positionMs = 27_416L,
                    durationMs = 365_683_639L
                )
            )
        ).jsonObject

        assertEquals("27", playInfo.getValue("position").jsonPrimitive.content)
        assertEquals("365683639", playInfo.getValue("duration").jsonPrimitive.content)
    }

    @Test
    fun `uses nonzero loading duration to avoid a full phone progress bar`() {
        val playInfo = Json.parseToJsonElement(
            CastNirvanaPlayInfoFormatter.format(
                CastPlaybackSnapshot(
                    state = CastTransportState.TRANSITIONING,
                    positionMs = 0L,
                    durationMs = 0L
                )
            )
        ).jsonObject

        assertEquals("0", playInfo.getValue("position").jsonPrimitive.content)
        assertEquals("1000", playInfo.getValue("duration").jsonPrimitive.content)
        assertEquals("2", playInfo.getValue("playerStatus").jsonPrimitive.content)
    }

    @Test
    fun `advertises danmaku and speed capability`() {
        val playInfo = Json.parseToJsonElement(
            CastNirvanaPlayInfoFormatter.format(CastPlaybackSnapshot(speed = 1.5f))
        ).jsonObject

        assertEquals("true", playInfo.getValue("supportMultiSpeed").jsonPrimitive.content)
        assertEquals("true", playInfo.getValue("supportVideoDanmaku").jsonPrimitive.content)
        assertEquals("true", playInfo.getValue("supportLiveDanmaku").jsonPrimitive.content)
        assertEquals("1.5", playInfo.getValue("speedInfo").jsonObject.getValue("currSpeed").jsonPrimitive.content)
    }

    @Test
    fun `nirvana service advertises danmaku and speed actions`() {
        val scpd = CastXmlDocuments.nirvanaControlScpd()

        assertTrue(scpd.contains("<name>SetSpeed</name>"))
        assertTrue(scpd.contains("<name>SetDanmakuSwitch</name>"))
        assertTrue(scpd.contains("<name>AppendDanmaku</name>"))
    }

    @Test
    fun `connection manager advertises prepare connection and octet stream sinks`() {
        val scpd = CastXmlDocuments.connectionManagerScpd()

        assertTrue(scpd.contains("<name>PrepareForConnection</name>"))
        assertTrue(CastXmlDocuments.SINK_PROTOCOL_INFO.contains("http-get:*:video/octet-stream:*"))
        assertTrue(CastXmlDocuments.SINK_PROTOCOL_INFO.contains("http-get:*:application/octet-stream:*"))
    }
}
