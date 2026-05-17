package dev.aaa1115910.bv.entity

import dev.aaa1115910.bv.util.PlayerUiTextFormatter
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthTransferFileTest {
    @Test
    fun `default file name includes uid timestamp and json extension`() {
        val authData = sampleAuthData(uid = 123456L)

        val fileName = AuthTransferFile.defaultFileName(
            authData = authData,
            now = Date(1_710_000_000_000L)
        )

        assertEquals("bv-auth-123456-20240309-160000.json", fileName)
    }

    @Test
    fun `encoded auth data can be decoded back to the same payload`() {
        val authData = sampleAuthData(uid = 42L)

        val encoded = AuthTransferFile.encode(authData)

        assertEquals(authData, AuthTransferFile.decode(encoded))
    }

    @Test
    fun `online count text uses exact count without plus suffix`() {
        assertEquals("321 人一起看", PlayerUiTextFormatter.onlineCount(321))
    }

    private fun sampleAuthData(uid: Long) = AuthData(
        uid = uid,
        uidCkMd5 = "uid-md5",
        sid = "sid",
        biliJct = "csrf",
        sessData = "sess",
        tokenExpiredData = 1_710_000_000_000L,
        accessToken = "access-token",
        refreshToken = "refresh-token"
    )
}
