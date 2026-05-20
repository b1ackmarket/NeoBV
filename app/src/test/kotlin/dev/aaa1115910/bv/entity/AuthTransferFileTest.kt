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

        assertEquals("neobv-data-123456-20240309-160000.json", fileName)
    }

    @Test
    fun `encoded transfer data can be decoded back to the same auth payload`() {
        val authData = sampleAuthData(uid = 42L)
        val settingsData = sampleSettingsData()

        val encoded = AuthTransferFile.encode(
            authData = authData,
            settingsData = settingsData
        )

        val decoded = AuthTransferFile.decodeTransfer(encoded)
        assertEquals(authData, decoded.authData)
        assertEquals(settingsData, decoded.settingsData)
    }

    @Test
    fun `legacy auth data can still be imported`() {
        val authData = sampleAuthData(uid = 24L)

        val decoded = AuthTransferFile.decodeTransfer(authData.toJson())

        assertEquals(authData, decoded.authData)
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

    private fun sampleSettingsData() = SettingsExportData(
        apiTypeLegacy = "Web",
        recommendationApiType = "Web",
        playbackApiType = "Web",
        enableProxy = false,
        proxyHttpServer = "",
        proxyGrpcServer = "",
        preferOfficialCdn = true,
        defaultQuality = 80,
        defaultLiveQuality = 10000,
        defaultVideoCodec = "HEVC",
        playerType = "Media3",
        enableSoftwareVideoDecoder = false,
        actionAfterPlay = 0,
        defaultAudio = 30280,
        enableFfmpegAudioRenderer = false,
        enableVolumeNormalization = false,
        defaultDanmakuTypes = listOf("Scroll", "Top", "Bottom"),
        defaultDanmakuScale = 1.5f,
        defaultDanmakuOpacity = 0.5f,
        defaultDanmakuSpeedFactor = 1f,
        defaultDanmakuArea = 1f,
        defaultDanmakuMask = false,
        defaultSubtitleFontSizeSp = 16,
        defaultSubtitleBackgroundOpacity = 0.4f,
        defaultSubtitleBottomPaddingDp = 24,
        defaultPlaySpeed = 100,
        seekStepSeconds = 10,
        showVideoInfo = true,
        showPersistentSeek = false,
        showPlayerStats = false,
        receiveAlphaUpdates = false,
        density = 1f,
        homeLeftNaviItem = "Home",
        firstHomeTopNavItem = 0,
        firstPersonalTopNavItem = "History",
        showHotword = true,
        incognitoMode = false
    )
}
