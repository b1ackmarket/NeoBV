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
    fun `current quality follows selected play url qn before reported fallback qn`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "current_quality": 250,
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
                              "base_url": "/live/current/index.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "qn=400&token=1" }
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

        assertEquals("https://line1.example.com/live/current/index.m3u8?qn=400&token=1", source?.playUrl)
        assertEquals(400, source?.currentQuality)
    }

    @Test
    fun `requested blue ray quality prefers codec route that actually supports qn`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 10000, "desc": "原画" },
                    { "qn": 400, "desc": "蓝光" },
                    { "qn": 250, "desc": "超清" }
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
                              "accept_qn": [10000, 250],
                              "base_url": "/live/avc.m3u8?",
                              "url_info": [
                                { "host": "https://avc.example.com", "extra": "qn=250" }
                              ]
                            },
                            {
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc.m3u8?",
                              "url_info": [
                                { "host": "https://hevc.example.com", "extra": "qn=400" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 400)

        assertEquals("https://hevc.example.com/live/hevc.m3u8?qn=400", source?.playUrl)
        assertEquals(400, source?.currentQuality)
    }

    @Test
    fun `requested quality prefers actual current qn before bitrate boost or high frame routes`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 25000, "desc": "原画真彩" },
                    { "qn": 10000, "desc": "原画" },
                    { "qn": 400, "desc": "蓝光" }
                  ],
                  "stream": [
                    {
                      "protocol_name": "http_hls",
                      "format": [
                        {
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "current_qn": 10000,
                              "accept_qn": [25000, 10000, 400],
                              "base_url": "/live/high-frame/index.m3u8?",
                              "url_info": [
                                { "host": "https://d1--cn-gotcha204b.bilivideo.com", "extra": "token=high-frame" }
                              ]
                            }
                          ]
                        },
                        {
                          "format_name": "ts",
                          "codec": [
                            {
                              "codec_name": "hevc",
                              "current_qn": 25000,
                              "accept_qn": [25000, 10000, 400],
                              "base_url": "/live/true-color/index.m3u8?",
                              "url_info": [
                                { "host": "https://line.example.com", "extra": "token=true-color" }
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

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            masterRequestedQn = 25000,
            preferHighBitrate = true
        )

        assertEquals(
            "https://line.example.com/live/true-color/index.m3u8?token=true-color",
            source?.playUrl
        )
        assertEquals(25000, source?.currentQuality)
    }

    @Test
    fun `unavailable requested quality falls back by qn before codec preference`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 25000, "desc": "原画真彩" },
                    { "qn": 10000, "desc": "原画" },
                    { "qn": 400, "desc": "蓝光" }
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
                              "accept_qn": [10000, 400],
                              "base_url": "/live/avc-original/index.m3u8?",
                              "url_info": [
                                { "host": "https://avc.example.com", "extra": "token=original" }
                              ]
                            },
                            {
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc-blue/index.m3u8?",
                              "url_info": [
                                { "host": "https://hevc.example.com", "extra": "token=blue" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 25000)

        assertEquals(
            "https://avc.example.com/live/avc-original/index.m3u8?token=original",
            source?.playUrl
        )
        assertEquals(10000, source?.currentQuality)
    }

    @Test
    fun `requested quality outranks route index in normal mode`() {
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
                              "current_qn": 10000,
                              "accept_qn": [10000],
                              "base_url": "/live/original/index.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "token=original" }
                              ]
                            },
                            {
                              "codec_name": "avc",
                              "current_qn": 25000,
                              "accept_qn": [25000],
                              "base_url": "/live/true-color/index.m3u8?",
                              "url_info": [
                                { "host": "https://line2.example.com", "extra": "token=true-color" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 25000)

        assertEquals(
            "https://line2.example.com/live/true-color/index.m3u8?token=true-color",
            source?.playUrl
        )
        assertEquals(25000, source?.currentQuality)
    }

    @Test
    fun `hevc live quality skips unsupported flv stream and uses hls route`() {
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
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?token=flv" }
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
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc/index.m3u8",
                              "url_info": [
                                { "host": "https://hls.example.com", "extra": "?token=hls" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 400)

        assertEquals("https://hls.example.com/live/hevc/index.m3u8?token=hls", source?.playUrl)
        assertEquals(400, source?.currentQuality)
    }

    @Test
    fun `av1 live quality skips unsupported flv stream and uses hls route`() {
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
                              "codec_name": "av1",
                              "current_qn": 10000,
                              "accept_qn": [10000],
                              "base_url": "/live/av1.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?token=flv" }
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
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "av1",
                              "current_qn": 10000,
                              "accept_qn": [10000],
                              "base_url": "/live/av1/index.m3u8",
                              "url_info": [
                                { "host": "https://hls.example.com", "extra": "?token=hls" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 10000)

        assertEquals("https://hls.example.com/live/av1/index.m3u8?token=hls", source?.playUrl)
        assertEquals(10000, source?.currentQuality)
    }

    @Test
    fun `high bitrate mode prefers fmp4 live bvc gotcha204b route`() {
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
                                { "host": "https://d1--cn-gotcha09.bilivideo.com", "extra": "?token=flv" }
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
                              "base_url": "/live-bvc/12345/live_12345.m3u8",
                              "url_info": [
                                { "host": "https://d1--cn-gotcha104.bilivideo.com", "extra": "?token=ts" }
                              ]
                            }
                          ]
                        },
                        {
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "base_url": "/live-bvc/12345/live_9adj_987/index.m3u8",
                              "url_info": [
                                { "host": "https://line.example.com", "extra": "?token=fmp4" },
                                { "host": "https://d1--cn-gotcha204b.bilivideo.com", "extra": "?token=fmp4" }
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

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            preferHighBitrate = true
        )

        assertEquals(
            "https://d1--cn-gotcha204b.bilivideo.com/live-bvc/12345/live_9adj_987/index.m3u8?token=fmp4",
            source?.playUrl
        )
        assertEquals("线路1（204b）", source?.lines?.firstOrNull()?.label)
    }

    @Test
    fun `high bitrate mode does not rewrite ts playlist host to gotcha204b`() {
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
                              "base_url": "/live-bvc/12345/live_9adj_987/index.m3u8",
                              "url_info": [
                                { "host": "https://d1--cn-gotcha09.bilivideo.com", "extra": "?token=ts" }
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

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            preferHighBitrate = true
        )

        assertEquals(
            "https://d1--cn-gotcha09.bilivideo.com/live-bvc/12345/live_9adj_987/index.m3u8?token=ts",
            source?.playUrl
        )
        assertEquals("线路1", source?.lines?.firstOrNull()?.label)
    }

    @Test
    fun `high bitrate mode does not synthesize gotcha204b when api does not provide it`() {
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
                          "format_name": "fmp4",
                          "codec": [
                            {
                              "codec_name": "avc",
                              "base_url": "/live-bvc/12345/live_9adj_987/index.m3u8",
                              "url_info": [
                                { "host": "https://d1--cn-gotcha01.bilivideo.com", "extra": "?token=fmp4" }
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

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            preferHighBitrate = true
        )

        assertEquals(
            "https://d1--cn-gotcha01.bilivideo.com/live-bvc/12345/live_9adj_987/index.m3u8?token=fmp4",
            source?.playUrl
        )
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
        assertEquals(listOf("蓝光", "高清"), source?.qualities?.map { it.desc })
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
            listOf("原画", "蓝光", "超清", "高清"),
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

        assertEquals(listOf("蓝光", "高清"), source?.qualities?.map { it.desc })
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
        assertEquals(listOf("蓝光"), source?.qualities?.map { it.desc })
    }

    @Test
    fun `live quality option labels keep official names only`() {
        assertEquals("原画真彩", LiveStreamResolver.buildQualityOptionLabel("原画真彩"))
        assertEquals("2K", LiveStreamResolver.buildQualityOptionLabel("2K"))
        assertEquals("4K", LiveStreamResolver.buildQualityOptionLabel("4K"))
        assertEquals("杜比", LiveStreamResolver.buildQualityOptionLabel("杜比"))
        assertEquals("原画", LiveStreamResolver.buildQualityOptionLabel("原画"))
        assertEquals("蓝光", LiveStreamResolver.buildQualityOptionLabel("蓝光"))
        assertNull(LiveStreamResolver.buildQualityOptionLabel("默认"))
    }

    @Test
    fun `live quality option labels ignore resolution and tag metadata`() {
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
            listOf("原画", "高清"),
            source?.qualities?.map { it.desc }
        )
        assertEquals(listOf("HDR", "高帧率"), source?.qualities?.firstOrNull()?.tags)
        assertEquals("1080P 原画", source?.qualities?.firstOrNull()?.playbackDesc)
    }

    @Test
    fun `playback quality label adds actual resolution prefix and tag suffix`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=10000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(LiveQualityOption(10000, "原画", tags = listOf("高帧率"))),
            currentQuality = 10000
        )

        assertEquals(
            "360P 原画（高帧率）",
            LiveStreamResolver.buildPlaybackQualityLabel(source, videoWidth = 640, videoHeight = 360)
        )
    }

    @Test
    fun `playback quality label uses official portrait compatibility floors`() {
        val tinyPortraitSource = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=10000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(LiveQualityOption(10000, "原画")),
            currentQuality = 10000
        )
        assertEquals(
            "360P 原画",
            LiveStreamResolver.buildPlaybackQualityLabel(
                tinyPortraitSource,
                videoWidth = 100,
                videoHeight = 178
            )
        )

        val portraitSource = tinyPortraitSource.copy(
            qualities = listOf(LiveQualityOption(10000, "原画")),
            currentQuality = 10000
        )
        assertEquals(
            "720P 原画",
            LiveStreamResolver.buildPlaybackQualityLabel(
                portraitSource,
                videoWidth = 540,
                videoHeight = 960
            )
        )
        assertEquals(
            "720P 原画",
            LiveStreamResolver.buildPlaybackQualityLabel(
                portraitSource,
                videoWidth = 720,
                videoHeight = 1280
            )
        )
        assertEquals(
            "1080P 原画",
            LiveStreamResolver.buildPlaybackQualityLabel(
                portraitSource,
                videoWidth = 1080,
                videoHeight = 1920
            )
        )
        assertEquals(
            "1080P 原画",
            LiveStreamResolver.buildPlaybackQualityLabel(
                portraitSource,
                videoWidth = 2160,
                videoHeight = 3840
            )
        )
    }

    @Test
    fun `playback quality label for menu option falls back to quality detail desc`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=10000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(
                LiveQualityOption(250, "超清", playbackDesc = "360P 超清"),
                LiveQualityOption(400, "蓝光", playbackDesc = "1080P 蓝光"),
                LiveQualityOption(10000, "原画", playbackDesc = "720P 原画")
            ),
            currentQuality = 10000
        )

        assertEquals(
            listOf("360P 超清", "1080P 蓝光", "720P 原画"),
            source.qualities.map { LiveStreamResolver.buildPlaybackQualityLabel(source, it) }
        )
    }

    @Test
    fun `playback quality label uses master display before official qn desc`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=25000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(LiveQualityOption(25000, "原画")),
            currentQuality = 25000,
            masterVariants = listOf(
                LiveMasterVariant(
                    qn = 25000,
                    display = "原画真彩",
                    width = 2560,
                    height = 1440,
                    bandwidth = 14_000_000L,
                    codecs = "avc1.640032",
                    stream = "avc",
                    url = "https://example.com/master-variant.m3u8?qn=25000"
                )
            )
        )

        assertEquals(
            "2K 原画真彩",
            LiveStreamResolver.buildPlaybackQualityLabel(source, videoWidth = 0, videoHeight = 0)
        )
    }

    @Test
    fun `playback quality label names original as high bitrate when true color quality coexists`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=10000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(
                LiveQualityOption(10000, "原画"),
                LiveQualityOption(25000, "原画真彩")
            ),
            currentQuality = 10000,
            masterVariants = listOf(
                LiveMasterVariant(
                    qn = 10000,
                    display = "原画",
                    width = 1920,
                    height = 1080,
                    bandwidth = 6_000_000L,
                    codecs = "avc1.640028",
                    stream = "avc",
                    url = "https://example.com/qn10000.m3u8"
                ),
                LiveMasterVariant(
                    qn = 25000,
                    display = "原画真彩",
                    width = 2560,
                    height = 1440,
                    bandwidth = 12_000_000L,
                    codecs = "avc1.640032",
                    stream = "avc",
                    url = "https://example.com/qn25000.m3u8"
                )
            )
        )

        assertEquals(
            "1080P 高码率",
            LiveStreamResolver.buildPlaybackQualityLabel(source, videoWidth = 1920, videoHeight = 1080)
        )
        assertEquals(
            "1080P 高码率",
            LiveStreamResolver.buildPlaybackQualityLabel(source, source.qualities.first { it.qn == 10000 })
        )
    }

    @Test
    fun `playback quality label names detail desc original as high bitrate when true color coexists`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=10000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(
                LiveQualityOption(10000, "原画", playbackDesc = "1080P 原画"),
                LiveQualityOption(25000, "原画真彩", playbackDesc = "2K 原画真彩")
            ),
            currentQuality = 10000
        )

        assertEquals(
            "1080P 高码率",
            LiveStreamResolver.buildPlaybackQualityLabel(source, source.qualities.first { it.qn == 10000 })
        )
    }

    @Test
    fun `playback quality label for menu option uses matching master variant`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=400",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(
                LiveQualityOption(250, "超清"),
                LiveQualityOption(400, "蓝光"),
                LiveQualityOption(25000, "原画真彩")
            ),
            currentQuality = 400,
            masterVariants = listOf(
                LiveMasterVariant(
                    qn = 250,
                    display = "超清",
                    width = 640,
                    height = 360,
                    bandwidth = 1_000_000L,
                    codecs = "avc1.64001f",
                    stream = "avc",
                    url = "https://example.com/qn250.m3u8"
                ),
                LiveMasterVariant(
                    qn = 400,
                    display = "蓝光",
                    width = 1920,
                    height = 1080,
                    bandwidth = 3_000_000L,
                    codecs = "avc1.640028",
                    stream = "avc",
                    url = "https://example.com/qn400.m3u8"
                ),
                LiveMasterVariant(
                    qn = 25000,
                    display = "原画真彩",
                    width = 1920,
                    height = 1080,
                    bandwidth = 8_000_000L,
                    codecs = "avc1.640028",
                    stream = "avc",
                    url = "https://example.com/qn25000-1080.m3u8"
                ),
                LiveMasterVariant(
                    qn = 25000,
                    display = "原画真彩",
                    width = 2560,
                    height = 1440,
                    bandwidth = 12_000_000L,
                    codecs = "avc1.640032",
                    stream = "avc",
                    url = "https://example.com/qn25000-2k.m3u8"
                )
            )
        )

        assertEquals(
            listOf("360P 超清", "1080P 蓝光", "2K 原画真彩"),
            source.qualities.map { LiveStreamResolver.buildPlaybackQualityLabel(source, it) }
        )
    }

    @Test
    fun `current master variant prefers current qn before same resolution true color variant`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=400",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(
                LiveQualityOption(400, "蓝光"),
                LiveQualityOption(25000, "原画真彩")
            ),
            currentQuality = 400,
            masterVariants = listOf(
                LiveMasterVariant(
                    qn = 25000,
                    display = "原画真彩",
                    width = 1920,
                    height = 1080,
                    bandwidth = 8_000_000L,
                    codecs = "avc1.640028",
                    stream = "avc",
                    url = "https://example.com/qn25000-1080.m3u8"
                ),
                LiveMasterVariant(
                    qn = 400,
                    display = "蓝光",
                    width = 1920,
                    height = 1080,
                    bandwidth = 3_000_000L,
                    codecs = "avc1.640028",
                    stream = "avc",
                    url = "https://example.com/qn400-1080.m3u8"
                )
            )
        )

        val variant = LiveStreamResolver.resolveCurrentMasterVariant(
            source = source,
            videoWidth = 1920,
            videoHeight = 1080
        )

        assertEquals(400, variant?.qn)
        assertEquals("蓝光", variant?.display)
    }

    @Test
    fun `playback quality label does not duplicate existing resolution semantics`() {
        val source = LivePlaybackSource(
            playUrl = "https://example.com/live/index.m3u8?qn=15000",
            lines = emptyList(),
            currentLineIndex = 0,
            qualities = listOf(LiveQualityOption(15000, "2K")),
            currentQuality = 15000
        )

        assertEquals(
            "2K",
            LiveStreamResolver.buildPlaybackQualityLabel(source, videoWidth = 2560, videoHeight = 1440)
        )

        val dolbySource = source.copy(
            currentQuality = 30000,
            qualities = listOf(LiveQualityOption(30000, "杜比"))
        )
        assertEquals(
            "杜比",
            LiveStreamResolver.buildPlaybackQualityLabel(dolbySource, videoWidth = 3840, videoHeight = 2160)
        )
    }

    @Test
    fun `master playlist parser keeps real variant metadata and infers names from resolution`() {
        val variants = LiveStreamResolver.parseMasterPlaylist(
            """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=9000000,FRAME-RATE=60,CODECS="avc1.640028,mp4a.40.2",RESOLUTION=1920x1080,BILI-QN=25000,BILI-DISPLAY="原画真彩",BILI-STREAM="avc"
            https://example.com/live/qn25000-1080/index.m3u8?qn=25000
            #EXT-X-STREAM-INF:BANDWIDTH=9000000,FRAME-RATE=60,CODECS="avc1.640028,mp4a.40.2",RESOLUTION=1920x1080,BILI-QN=25000,BILI-DISPLAY="原画真彩",BILI-STREAM="avc"
            https://backup.example.com/live/qn25000-1080/index.m3u8?qn=25000
            #EXT-X-STREAM-INF:BANDWIDTH=14000000,FRAME-RATE=60,CODECS="avc1.640032",RESOLUTION=2560x1440,BILI-QN=25000,BILI-DISPLAY="原画真彩",BILI-STREAM="avc"
            https://example.com/live/qn25000-2k/index.m3u8?qn=25000
            #EXT-X-STREAM-INF:BANDWIDTH=24000000,FRAME-RATE=60,CODECS="hev1.1.6.L150",RESOLUTION=3840x2160,BILI-QN=25000,BILI-DISPLAY="原画真彩",BILI-STREAM="hevc"
            https://example.com/live/qn25000-4k/index.m3u8?qn=25000
            """.trimIndent()
        )

        assertEquals(3, variants.size)
        assertEquals(listOf(3840, 2560, 1920), variants.map { it.width })
        assertEquals(listOf("4K 原画真彩", "2K 原画真彩", "1080P 原画真彩"), variants.map { it.inferredDisplayName })
        assertEquals(listOf(60f, 60f, 60f), variants.map { it.frameRate })
        assertEquals("原画真彩", variants[0].display)
        assertEquals("hev1.1.6.L150", variants[0].codecs)
        assertEquals("hevc", variants[0].stream)
        assertEquals("https://example.com/live/qn25000-4k/index.m3u8?qn=25000", variants[0].url)
    }

    @Test
    fun `playable source keeps avc master playlist as observation without changing selected play url`() {
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
                              "base_url": "/live/current/index.m3u8?",
                              "url_info": [
                                { "host": "https://line1.example.com", "extra": "qn=250" }
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
        val masterVariants = listOf(
            LiveMasterVariant(
                qn = 25000,
                display = "原画真彩",
                width = 2560,
                height = 1440,
                bandwidth = 14_000_000L,
                codecs = "avc1.640032",
                stream = "avc",
                url = "https://example.com/master-variant.m3u8?qn=25000"
            )
        )

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            masterVariants = masterVariants,
            masterRequestedQn = 25000
        )

        assertEquals("https://line1.example.com/live/current/index.m3u8?qn=250", source?.playUrl)
        assertEquals(250, source?.currentQuality)
        assertEquals(25000, source?.masterRequestedQn)
        assertEquals(masterVariants, source?.masterVariants)
    }

    @Test
    fun `playable source prefers matching hevc master playlist variant as play url`() {
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
                              "codec_name": "hevc",
                              "current_qn": 25000,
                              "accept_qn": [25000],
                              "base_url": "/live/hevc.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?qn=25000" }
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
        val masterVariants = listOf(
            LiveMasterVariant(
                qn = 25000,
                display = "原画真彩",
                width = 2560,
                height = 1440,
                bandwidth = 14_000_000L,
                codecs = "hev1.1.6.L150",
                stream = "hevc",
                url = "https://example.com/hevc-master-variant.m3u8?qn=25000"
            )
        )

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            masterVariants = masterVariants,
            masterRequestedQn = 25000
        )

        assertEquals("https://example.com/hevc-master-variant.m3u8?qn=25000", source?.playUrl)
        assertEquals(25000, source?.currentQuality)
        assertEquals("hevc", source?.lines?.firstOrNull()?.codecName)
    }

    @Test
    fun `hevc only requested quality keeps selected qn instead of falling to lower qn`() {
        val playInfo = Json.parseToJsonElement(
            """
            {
              "playurl_info": {
                "playurl": {
                  "g_qn_desc": [
                    { "qn": 400, "desc": "蓝光" },
                    { "qn": 250, "desc": "超清" }
                  ],
                  "stream": [
                    {
                      "protocol_name": "http_stream",
                      "format": [
                        {
                          "format_name": "flv",
                          "codec": [
                            {
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc-blue.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?qn=400" }
                              ]
                            },
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "accept_qn": [250],
                              "base_url": "/live/avc-super.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?qn=250" }
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

        val source = LiveStreamResolver.resolvePlayableSource(playInfo, masterRequestedQn = 400)

        assertEquals("https://flv.example.com/live/hevc-blue.flv?qn=400", source?.playUrl)
        assertEquals(400, source?.currentQuality)
        assertEquals("hevc", source?.lines?.firstOrNull()?.codecName)
    }

    @Test
    fun `hevc only requested quality prefers same qn master before raw flv attempt`() {
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
                              "codec_name": "hevc",
                              "current_qn": 400,
                              "accept_qn": [400],
                              "base_url": "/live/hevc-blue.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?qn=400" }
                              ]
                            },
                            {
                              "codec_name": "avc",
                              "current_qn": 250,
                              "accept_qn": [250],
                              "base_url": "/live/avc-super.flv",
                              "url_info": [
                                { "host": "https://flv.example.com", "extra": "?qn=250" }
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
        val masterVariants = listOf(
            LiveMasterVariant(
                qn = 400,
                display = "蓝光",
                width = 1920,
                height = 1080,
                bandwidth = 4_000_000L,
                codecs = "hev1.1.6.L120,mp4a.40.2",
                stream = "hevc",
                url = "https://master.example.com/hevc-blue/index.m3u8?qn=400"
            )
        )

        val source = LiveStreamResolver.resolvePlayableSource(
            playInfo = playInfo,
            masterVariants = masterVariants,
            masterRequestedQn = 400
        )

        assertEquals("https://master.example.com/hevc-blue/index.m3u8?qn=400", source?.playUrl)
        assertEquals(400, source?.currentQuality)
        assertEquals("hevc", source?.lines?.firstOrNull()?.codecName)
    }
}
