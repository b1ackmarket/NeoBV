package dev.aaa1115910.biliapi.http.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiSignTest {
    @Test
    fun `live danmaku info endpoint uses wbi signing`() {
        assertTrue(
            shouldUseWbiSignForGetPath("/xlive/web-room/v1/index/getDanmuInfo")
        )
    }

    @Test
    fun `user seasons series endpoint uses wbi signing`() {
        assertTrue(
            shouldUseWbiSignForGetPath("/x/polymer/web-space/seasons_series_list")
        )
    }

    @Test
    fun `ordinary live endpoints do not use wbi signing`() {
        assertFalse(
            shouldUseWbiSignForGetPath("/xlive/web-room/v2/index/getRoomPlayInfo")
        )
    }
}
