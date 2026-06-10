package dev.aaa1115910.bv.cast.protocol

import io.ktor.http.Parameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertNull(content.quality)
        assertEquals(false, content.danmakuEnabled)
    }

    @Test
    fun `parse live cast ignores restricted mobile quality`() {
        val content = CastContentParser.parse(
            path = "/bilibili/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = """
                <CurrentURI>http://example.com/live.flv?qn=250&amp;bili_room_id=865961&amp;proj_source=bilibili</CurrentURI>
                <upnp:longDescription>{"content":{"contentType":3,"roomId":865961,"userDesireQn":116,"danmakuSwitchSave":true}}</upnp:longDescription>
            """.trimIndent()
        )

        assertNotNull(content)
        assertEquals(865961, content.roomId)
        assertNull(content.quality)
        assertEquals(true, content.danmakuEnabled)
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

    @Test
    fun `parse standard DLNA direct media url from CurrentURI`() {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>http://videoplay.115.com/m3u8/pickcode?filesha1=abc&amp;definition=5</CurrentURI>
                  <CurrentURIMetaData><![CDATA[
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
                      <item id="1" parentID="0" restricted="1">
                        <dc:title>115 video</dc:title>
                        <upnp:class>object.item.videoItem</upnp:class>
                        <res protocolInfo="http-get:*:video/mp4:*">http://videoplay.115.com/m3u8/pickcode?filesha1=abc&amp;definition=5</res>
                      </item>
                    </DIDL-Lite>
                  ]]></CurrentURIMetaData>
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
        assertNull(content.aid)
        assertEquals("115 video", content.title)
        assertEquals(CastDirectMediaType.Hls, content.directMediaType)
        assertEquals(
            "http://videoplay.115.com/m3u8/pickcode?filesha1=abc&definition=5",
            content.directMediaUrl
        )
    }

    @Test
    fun `parse standard DLNA audio metadata with album art`() {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>https://music.example.com/song.mp3</CurrentURI>
                  <CurrentURIMetaData><![CDATA[
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
                      <item id="1" parentID="0" restricted="1">
                        <dc:title>Song title</dc:title>
                        <dc:creator>Song artist</dc:creator>
                        <upnp:artist>Upnp artist</upnp:artist>
                        <upnp:albumArtURI>https://music.example.com/cover.jpg</upnp:albumArtURI>
                        <upnp:class>object.item.audioItem.musicTrack</upnp:class>
                        <res protocolInfo="http-get:*:audio/mpeg:*">https://music.example.com/song.mp3</res>
                      </item>
                    </DIDL-Lite>
                  ]]></CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body
        )

        assertNotNull(content)
        assertEquals("Song title", content.title)
        assertEquals("Upnp artist", content.creator)
        assertEquals(CastDirectMediaType.Audio, content.directMediaType)
        assertEquals("https://music.example.com/cover.jpg", content.directMediaCover)
        assertEquals("https://music.example.com/song.mp3", content.directMediaUrl)
    }

    @Test
    fun `parse DLNA audio album art from element attribute`() {
        val body = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <CurrentURI>https://music.example.com/song.flac</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"&gt;&lt;item&gt;&lt;dc:title&gt;Song title&lt;/dc:title&gt;&lt;upnp:albumArtURI dlna:profileID="JPEG_TN" xmlns:dlna="urn:schemas-dlna-org:metadata-1-0/" src="https://music.example.com/cover-attr.jpg"/&gt;&lt;res protocolInfo="http-get:*:audio/flac:*"&gt;https://music.example.com/song.flac&lt;/res&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body
        )

        assertNotNull(content)
        assertEquals(CastDirectMediaType.Audio, content.directMediaType)
        assertEquals("https://music.example.com/cover-attr.jpg", content.directMediaCover)
    }

    @Test
    fun `parse netease style DLNA audio item`() {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>https://m701.music.126.net/song.m4a?authSecret=abc</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"&gt;&lt;item id="0" parentID="-1" restricted="1"&gt;&lt;dc:title&gt;网易云音乐&lt;/dc:title&gt;&lt;upnp:artist&gt;歌手&lt;/upnp:artist&gt;&lt;upnp:album&gt;专辑&lt;/upnp:album&gt;&lt;upnp:albumArtURI&gt;https://p1.music.126.net/cover.jpg?param=512y512&lt;/upnp:albumArtURI&gt;&lt;res protocolInfo="http-get:*:audio/mp4:DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"&gt;https://m701.music.126.net/song.m4a?authSecret=abc&lt;/res&gt;&lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body
        )

        assertNotNull(content)
        assertEquals("网易云音乐", content.title)
        assertEquals("歌手", content.creator)
        assertEquals(CastDirectMediaType.Audio, content.directMediaType)
        assertEquals("https://p1.music.126.net/cover.jpg?param=512y512", content.directMediaCover)
        assertEquals("https://m701.music.126.net/song.m4a?authSecret=abc", content.directMediaUrl)
    }

    @Test
    fun `direct media url does not replace bilibili projection identity`() {
        val body = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <CurrentURI>http://upos-sz-mirrorcoso1.bilivideo.com/video.mp4?aid=116662971925516&amp;cid=38721687073</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/"&gt;&lt;item&gt;&lt;dc:title&gt;PiliPlus video&lt;/dc:title&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/bilibili/bilibili/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body
        )

        assertNotNull(content)
        assertEquals(116662971925516L, content.aid)
        assertEquals(38721687073L, content.cid)
        assertEquals(
            "http://upos-sz-mirrorcoso1.bilivideo.com/video.mp4?aid=116662971925516&cid=38721687073",
            content.directMediaUrl
        )
    }

    @Test
    fun `parse PiliPlus dart DLNA bilibili direct media hint`() {
        val body = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <CurrentURI>https://upos-sz-mirrorcoso1.bilivideo.com/upgcxcode/video.m4s?deadline=1&amp;oi=2</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/"&gt;&lt;item&gt;&lt;dc:title&gt;PiliPlus 投屏视频&lt;/dc:title&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/bilibili/bilibili/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body,
            headers = mapOf("User-Agent" to "Dart/3.12 (dart:io)")
        )

        assertNotNull(content)
        assertNull(content.aid)
        assertEquals(CastClientHint.PiliPlus, content.clientHint)
        assertEquals("PiliPlus 投屏视频", content.title)
        assertEquals(
            "https://upos-sz-mirrorcoso1.bilivideo.com/upgcxcode/video.m4s?deadline=1&oi=2",
            content.directMediaUrl
        )
        assertEquals(true, content.isBilibiliDirectMedia)
    }

    @Test
    fun `BiliPai DIDL creator stays generic bilibili direct media`() {
        val body = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <CurrentURI>https://upos-sz-mirrorcoso1.bilivideo.com/video.mp4</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"&gt;&lt;item id="1" parentID="0" restricted="1"&gt;&lt;dc:title&gt;BiliPai Video&lt;/dc:title&gt;&lt;dc:creator&gt;BiliPai&lt;/dc:creator&gt;&lt;res protocolInfo="http-get:*:video/mp4:*"&gt;https://upos-sz-mirrorcoso1.bilivideo.com/video.mp4&lt;/res&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
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
        assertEquals(CastClientHint.GenericBilibili, content.clientHint)
        assertEquals("BiliPai", content.creator)
        assertEquals(true, content.isBilibiliDirectMedia)
    }

    @Test
    fun `official bilibili projection has priority over dart headers`() {
        val body = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <CurrentURI>https://upos-sz-mirrorcoso1.bilivideo.com/video.mp4?proj_source=bilibili&amp;aid=116662971925516&amp;cid=38721687073</CurrentURI>
                  <CurrentURIMetaData>&lt;DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/"&gt;&lt;item&gt;&lt;dc:title&gt;Official&lt;/dc:title&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val content = CastContentParser.parse(
            path = "/bilibili/AVTransport/control",
            queryParameters = Parameters.Empty,
            body = body,
            headers = mapOf("User-Agent" to "Dart/3.12 (dart:io)")
        )

        assertNotNull(content)
        assertEquals(CastClientHint.OfficialBilibili, content.clientHint)
    }
}
