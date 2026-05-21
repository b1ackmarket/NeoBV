package dev.aaa1115910.bv.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveStreamResolverTest {
    @Test
    fun `default live url chooses http stream before hls stream`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "stream": [
                    {
                      "protocol_name": "http_stream",
                      "format": [
                        {
                          "format_name": "flv",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "base_url": "/live-bvc/12345/live_12345.flv",
                              "url_info": [
                                {
                                  "host": "https://flv.example.com",
                                  "extra": "?token=flv"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "base_url": "/live-bvc/12345/live_12345/index.m3u8",
                              "url_info": [
                                {
                                  "host": "https://hls.example.com",
                                  "extra": "?token=hls"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(
            "https://flv.example.com/live-bvc/12345/live_12345.flv?token=flv",
            LiveStreamResolver.resolvePlayableUrl(playInfo)
        )

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)
        assertEquals(1, source?.lines?.size)
        assertEquals(0, source?.currentLineIndex)
        assertEquals("线路1", source?.lines?.firstOrNull()?.label)
    }

    @Test
    fun `playable live url falls back to durl when stream list is missing`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "durl": [
                {
                  "url": "https://fallback.example.com/live_12345.flv"
                }
              ]
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(
            "https://fallback.example.com/live_12345.flv",
            LiveStreamResolver.resolvePlayableUrl(playInfo)
        )
    }

    @Test
    fun `room id resolver prefers api room id and falls back to requested value`() {
        val roomInfo = Json.parseToJsonElement(
            """
            {
              "room_id": 987654,
              "live_status": 1
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(987654, LiveStreamResolver.resolveRoomId(roomInfo, fallbackRoomId = 123456))
        assertEquals(123456, LiveStreamResolver.resolveRoomId(null, fallbackRoomId = 123456))
    }

    @Test
    fun `playable live url returns null when no candidate exists`() {
        val playInfo = Json.parseToJsonElement("{}").jsonObject

        assertNull(LiveStreamResolver.resolvePlayableUrl(playInfo))
    }

    @Test
    fun `playable source keeps quality options and requested line index`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "current_quality": 400,
              "quality_description": [
                { "qn": 400, "desc": "蓝光" },
                { "qn": 150, "desc": "高清" }
              ],
              "playurl_info": {
                "playurl": {
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "base_url": "/a/index.m3u8",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "?a=1" },
                                { "host": "https://line2.example.com", "extra": "?b=2" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, requestedLineIndex = 1)

        assertEquals("https://line2.example.com/a/index.m3u8?b=2", source?.playUrl)
        assertEquals(400, source?.currentQuality)
        assertEquals(listOf("1080P蓝光", "480P高清"), source?.qualities?.map { it.desc })
        assertEquals(1, source?.currentLineIndex)
    }

    @Test
    fun `playable source extracts current quality and g qn desc from live room play info v2`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 10000, "desc": "原画" },
                    { "qn": 400, "desc": "蓝光" },
                    { "qn": 250, "desc": "超清" },
                    { "qn": 150, "desc": "高清" }
                  ],
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "base_url": "/live-bvc/demo/index.m3u8?",
                              "url_info": [
                                {
                                  "host": "https://line1.example.com",
                                  "extra": "token=1"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)

        assertEquals("https://line1.example.com/live-bvc/demo/index.m3u8?token=1", source?.playUrl)
        assertEquals(250, source?.currentQuality)
        assertEquals(
            listOf("1080P原画", "1080P蓝光", "720P超清", "480P高清"),
            source?.qualities?.map { it.desc }
        )
    }

    @Test
    fun `playable source filters quality list to actually accepted qn values when provided`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 10000, "desc": "原画" },
                    { "qn": 400, "desc": "蓝光" },
                    { "qn": 250, "desc": "超清" },
                    { "qn": 150, "desc": "高清" }
                  ],
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 400,
                              "accept_qn": [400, 150],
                              "base_url": "/live-bvc/demo/index.m3u8?",
                              "url_info": [
                                {
                                  "host": "https://line1.example.com",
                                  "extra": "token=1"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)

        assertEquals(listOf("1080P蓝光", "480P高清"), source?.qualities?.map { it.desc })
        assertEquals(400, source?.currentQuality)
    }

    @Test
    fun `live lines collapse format codec variants into actual routes and prefer avc ts for hls stability`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "base_url": "/live/ts-avc.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "route=1" },
                                { "host": "https://line2.example.com", "extra": "route=2" }
                              ]
                            },
                            {
                              "codec_name": "hevc",
                              "current_qn": 250,
                              "base_url": "/live/ts-hevc.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "route=1" },
                                { "host": "https://line2.example.com", "extra": "route=2" }
                              ]
                            }
                          ]
                        },
                        {
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "base_url": "/live/fmp4-avc/index.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "route=1" },
                                { "host": "https://line2.example.com", "extra": "route=2" }
                              ]
                            },
                            {
                              "codec_name": "hevc",
                              "current_qn": 250,
                              "base_url": "/live/fmp4-hevc/index.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "route=1" },
                                { "host": "https://line2.example.com", "extra": "route=2" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, requestedLineIndex = 1)

        assertEquals(2, source?.lines?.size)
        assertEquals("https://line2.example.com/live/ts-avc.m3u8?route=2", source?.playUrl)
        assertEquals(
            listOf(
                "https://line1.example.com/live/ts-avc.m3u8?route=1",
                "https://line2.example.com/live/ts-avc.m3u8?route=2"
            ),
            source?.lines?.map { it.url }
        )
    }

    @Test
    fun `live lines hide mcdn backup when normal routes are available`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "base_url": "/live/ts-avc.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "route=1" },
                                { "host": "https://mcdn.example.com", "extra": "route=2" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)

        assertEquals(1, source?.lines?.size)
        assertEquals("https://line1.example.com/live/ts-avc.m3u8?route=1", source?.playUrl)
    }

    @Test
    fun `playable source ignores malformed array entries instead of throwing json object errors`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    0,
                    { "qn": 400, "desc": "蓝光" }
                  ],
                  "stream": [
                    0,
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        0,
                        {
                          "format_name": "ts",
                          "codec": [
                            0,
                            {
                              "codec_name": "avc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/ts-avc.m3u8?",
                              "url_info": [
                                0,
                                { "host": "https://line1.example.com", "extra": "route=1" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)

        assertEquals("https://line1.example.com/live/ts-avc.m3u8?route=1", source?.playUrl)
        assertEquals(listOf("1080P蓝光"), source?.qualities?.map { it.desc })
    }

    @Test
    fun `live qualities use web style display names for common qn values`() {
        assertEquals("1080P高码率", LiveStreamResolver.normalizeLiveQualityDesc(25000, "原画真彩"))
        assertEquals("1080P原画", LiveStreamResolver.normalizeLiveQualityDesc(10000, "原画"))
        assertEquals("1080P蓝光", LiveStreamResolver.normalizeLiveQualityDesc(400, "蓝光"))
        assertEquals("720P超清", LiveStreamResolver.normalizeLiveQualityDesc(250, "超清"))
        assertEquals("480P高清", LiveStreamResolver.normalizeLiveQualityDesc(150, "高清"))
        assertEquals("360P流畅", LiveStreamResolver.normalizeLiveQualityDesc(80, "流畅"))
        assertEquals("2K原画", LiveStreamResolver.normalizeLiveQualityDesc(15000, "2K"))
        assertEquals("4K原画", LiveStreamResolver.normalizeLiveQualityDesc(20000, "4K"))
        assertEquals("杜比视界", LiveStreamResolver.normalizeLiveQualityDesc(30000, "杜比"))
    }

    @Test
    fun `live quality display names include hdr and high frame tags from media base desc`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    {
                      "qn": 10000,
                      "desc": "原画",
                      "hdr_desc": "HDR",
                      "media_base_desc": {
                        "detail_desc": {
                          "desc": "1080P 原画",
                          "tag": ["高帧率"]
                        }
                      }
                    },
                    {
                      "qn": 150,
                      "desc": "高清",
                      "media_base_desc": {
                        "detail_desc": {
                          "desc": "480P 高清"
                        }
                      }
                    }
                  ],
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 10000,
                              "accept_qn": [10000, 150],
                              "base_url": "/live/ts-avc.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "token=1" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val source = LiveStreamResolver.resolvePlayableSource(playInfo)

        assertEquals(
            listOf("1080P原画（HDR高帧率）", "480P高清"),
            source?.qualities?.map { it.desc }
        )
    }

    @Test
    fun `live quality names use original suffix for 2k and 4k and explicit 360p for smooth`() {
        assertEquals("360P流畅", LiveStreamResolver.normalizeLiveQualityDesc(80, "流畅"))
        assertEquals("2K原画", LiveStreamResolver.normalizeLiveQualityDesc(15000, "2K"))
        assertEquals("4K原画", LiveStreamResolver.normalizeLiveQualityDesc(20000, "4K"))
    }
}
