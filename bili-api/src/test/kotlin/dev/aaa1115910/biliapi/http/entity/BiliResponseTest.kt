package dev.aaa1115910.biliapi.http.entity

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BiliResponseTest {
    @Test
    fun `response message defaults to empty string when omitted`() {
        val response = Json.decodeFromString<BiliResponse<String>>(
            """{"code":0,"data":"ok"}"""
        )

        assertEquals("", response.message)
        assertEquals("ok", response.getResponseData())
    }
}
