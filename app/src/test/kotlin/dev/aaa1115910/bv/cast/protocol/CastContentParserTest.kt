package dev.aaa1115910.bv.cast.protocol

import io.ktor.http.Parameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CastContentParserTest {
    @Test
    fun `parse projection analytics style JSON body`() {
        val content = CastContentParser.parse(
            path = "/bilibili/NirvanaControl/control",
            queryParameters = Parameters.Empty,
            body = """
                {
                  "aid": "116662971925516",
                  "cid": "38721687073",
                  "seekTs": "36",
                  "userDesireQn": "120",
                  "userDesireSpeed": "1.5",
                  "epId": "0",
                  "seasonId": "0"
                }
            """.trimIndent()
        )

        assertNotNull(content)
        assertEquals(116662971925516L, content.aid)
        assertEquals(38721687073L, content.cid)
        assertEquals(36, content.seekSeconds)
        assertEquals(120, content.quality)
        assertEquals(1.5f, content.playSpeed)
    }

    @Test
    fun `prefer official desired quality and parse danmaku switch`() {
        val content = CastContentParser.parse(
            path = "/bilibili/NirvanaControl/control",
            queryParameters = Parameters.Empty,
            body = """
                {
                  "aid": "116662971925516",
                  "cid": "38721687073",
                  "qn": "64",
                  "userDesireQn": "120",
                  "userDesireSpeed": "2.0",
                  "danmakuSwitchSave": "false"
                }
            """.trimIndent()
        )

        assertNotNull(content)
        assertEquals(120, content.quality)
        assertEquals(2.0f, content.playSpeed)
        assertEquals(false, content.danmakuEnabled)
    }

    @Test
    fun `parse query parameters with bvid and cid`() {
        val content = CastContentParser.parse(
            path = "/cast/open",
            queryParameters = Parameters.build {
                append("bvid", "BV1xx411c7mD")
                append("cid", "1234")
                append("part_title", "%E6%AD%A3%E7%89%87")
            },
            body = null
        )

        assertNotNull(content)
        assertEquals("BV1xx411c7mD", content.bvid)
        assertEquals(1234L, content.cid)
        assertEquals("正片", content.partTitle)
    }

    @Test
    fun `parse live room identity from form body`() {
        val content = CastContentParser.parse(
            path = "/bilibili/live",
            queryParameters = Parameters.Empty,
            body = "room_id=27183290&title=%E7%9B%B4%E6%92%AD&userDesireQn=10000&dm_switch=0"
        )

        assertNotNull(content)
        assertEquals(27183290, content.roomId)
        assertEquals("直播", content.title)
        assertEquals(10000, content.quality)
        assertEquals(false, content.danmakuEnabled)
    }

    @Test
    fun `parse official bilibili projection DIDL long description`() {
        val longDescription =
            "_wHTR8YnWOvCScdp30aTVk7OkqFHJ-ZH9fLX1HiLdvQ-Po5GMw9paJnjOOsb8TrFL1hYMekHKzVEdBjC8zmnlxZ1unMb2bZC0gbqmvLkqOyYz57iURnuDCFJIbMfZ7948GGdplio23x_ovwyhJcBO6uKtcOQVJD5MvhT0-nJsKusGgpRNCQVjJ0BmeEgmG73JSmxmzck811ZHA5i7dk7uCk72EeOvMpiyZIURjPUFhJgGStb2ymiulAd020jCnmptS6nMCroCoN2bQXzHIIfi9iIqqm_7Ux4zU1gt2ct_X-KsoUk6ZWjTb3_15qOFPcPcPfQTEhoeZIiGyMuzSW9aQOqgHeawEowV0SuaQHTCCfGZ5r-QLbxV2yyhXzE6SeJ5NUygZ-UuBwYw1krjkkxFvGulp_8lNv9ubhs5P_QjZaHGp1x7efwOvyTosuFmatBCAHLNEd3VV65cj-5dIQQo_SAAyePxOev_fnA9__guxZAM1-isDgzMXOEGLN87stWdNaR5TB9_S9XMlEDscVhsS_Cy0pKOpEhE9NE_A56dQ9--9D_kgqRLxeZkcLm8UMIBW2eg-BaWt2iHUBHZ0P3yIqaC_PtCFpN6tU9PKfKNGBCq9mv3_XYWmQDH1DcN1XNcUn5VbfJVpbqLVhwVRr-5w"
        val body = """
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>bilibili://projection?proj_source=bilibili&amp;_nva_ext_=</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"&gt;&lt;item id="0" parentID="-1" restricted="1"&gt;&lt;dc:title&gt;如果罪恶都市变得非常阴险，大结局&lt;/dc:title&gt;&lt;upnp:longDescription&gt;$longDescription&lt;/upnp:longDescription&gt;&lt;res protocolInfo="http-get:*:video/x-flv:DLNA.ORG_OP=01;DLNA.ORG_CI=0"&gt;bilibili://projection?proj_source=bilibili&amp;amp;_nva_ext_=&lt;/res&gt;&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/bilibili/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body
        )

        assertNotNull(content)
        assertEquals(116622152963643L, content.aid)
        assertEquals(38546702683L, content.cid)
        assertEquals(1, content.seekSeconds)
        assertEquals(16, content.quality)
        assertEquals("如果罪恶都市变得非常阴险，大结局", content.title)
    }
}
