package dev.aaa1115910.bv.entity

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.component.HomePageSettingItem
import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.entity.live.LiveDefaultQuality
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.screen.settings.content.ActionAfterPlayItems
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.RecommendationApiType
import dev.aaa1115910.bv.viewmodel.player.SeekStepOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AuthTransferFile {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun encode(
        authData: AuthData,
        settingsData: SettingsExportData = SettingsExportData.fromPrefs()
    ): String = json.encodeToString(
        AppDataTransferFile(
            authData = authData,
            settingsData = settingsData
        )
    )

    fun decode(content: String): AuthData = AuthData.fromJson(content.trim())

    fun decodeTransfer(content: String): AppDataTransferFile {
        val normalized = content.trim()
        return runCatching {
            json.decodeFromString<AppDataTransferFile>(normalized)
        }.getOrElse {
            AppDataTransferFile(authData = AuthData.fromJson(normalized))
        }
    }

    fun defaultFileName(
        authData: AuthData,
        now: Date = Date()
    ): String {
        val timestamp = fileNameDateFormat.format(now)
        return "neobv-data-${authData.uid}-$timestamp.json"
    }
}

@Serializable
data class AppDataTransferFile(
    @SerialName("schema_version")
    val schemaVersion: Int = 2,
    @SerialName("auth")
    val authData: AuthData,
    @SerialName("settings")
    val settingsData: SettingsExportData? = null
)

@Serializable
data class SettingsExportData(
    @SerialName("api_type_legacy")
    val apiTypeLegacy: String,
    @SerialName("recommendation_api_type")
    val recommendationApiType: String,
    @SerialName("enable_personalized_recommendation")
    val enablePersonalizedRecommendation: Boolean = true,
    @SerialName("playback_api_type")
    val playbackApiType: String,
    @SerialName("enable_proxy")
    val enableProxy: Boolean,
    @SerialName("proxy_http_server")
    val proxyHttpServer: String,
    @SerialName("proxy_grpc_server")
    val proxyGrpcServer: String,
    @SerialName("prefer_official_cdn")
    val preferOfficialCdn: Boolean,
    @SerialName("default_quality")
    val defaultQuality: Int,
    @SerialName("default_live_quality")
    val defaultLiveQuality: Int,
    @SerialName("default_video_codec")
    val defaultVideoCodec: String,
    @SerialName("player_type")
    val playerType: String,
    @SerialName("enable_software_video_decoder")
    val enableSoftwareVideoDecoder: Boolean,
    @SerialName("action_after_play")
    val actionAfterPlay: Int,
    @SerialName("default_audio")
    val defaultAudio: Int,
    @SerialName("enable_ffmpeg_audio_renderer")
    val enableFfmpegAudioRenderer: Boolean,
    @SerialName("enable_volume_normalization")
    val enableVolumeNormalization: Boolean,
    @SerialName("default_danmaku_types")
    val defaultDanmakuTypes: List<String>,
    @SerialName("default_danmaku_scale")
    val defaultDanmakuScale: Float,
    @SerialName("default_danmaku_opacity")
    val defaultDanmakuOpacity: Float,
    @SerialName("default_danmaku_speed_factor")
    val defaultDanmakuSpeedFactor: Float,
    @SerialName("default_danmaku_area")
    val defaultDanmakuArea: Float,
    @SerialName("default_danmaku_mask")
    val defaultDanmakuMask: Boolean,
    @SerialName("default_subtitle_font_size_sp")
    val defaultSubtitleFontSizeSp: Int,
    @SerialName("default_subtitle_background_opacity")
    val defaultSubtitleBackgroundOpacity: Float,
    @SerialName("default_subtitle_bottom_padding_dp")
    val defaultSubtitleBottomPaddingDp: Int,
    @SerialName("default_play_speed")
    val defaultPlaySpeed: Int,
    @SerialName("seek_step_seconds")
    val seekStepSeconds: Int,
    @SerialName("show_video_info")
    val showVideoInfo: Boolean,
    @SerialName("show_persistent_seek")
    val showPersistentSeek: Boolean,
    @SerialName("show_player_stats")
    val showPlayerStats: Boolean,
    @SerialName("receive_alpha_updates")
    val receiveAlphaUpdates: Boolean,
    @SerialName("density")
    val density: Float,
    @SerialName("home_left_nav_item")
    val homeLeftNaviItem: String,
    @SerialName("first_home_top_nav_item")
    val firstHomeTopNavItem: Int,
    @SerialName("first_personal_top_nav_item")
    val firstPersonalTopNavItem: String,
    @SerialName("show_hotword")
    val showHotword: Boolean,
    @SerialName("incognito_mode")
    val incognitoMode: Boolean
) {
    fun saveToPrefs() {
        Prefs.apiType = enumValueOrDefault(apiTypeLegacy, ApiType.Web)
        Prefs.recommendationApiType =
            enumValueOrDefault(recommendationApiType, RecommendationApiType.App)
        Prefs.enablePersonalizedRecommendation = enablePersonalizedRecommendation
        Prefs.playbackApiType = enumValueOrDefault(playbackApiType, ApiType.Web)
        Prefs.enableProxy = enableProxy
        Prefs.proxyHttpServer = proxyHttpServer
        Prefs.proxyGRPCServer = proxyGrpcServer
        Prefs.preferOfficialCdn = preferOfficialCdn
        Prefs.defaultQuality = Resolution.fromCode(defaultQuality)
        Prefs.defaultLiveQuality = LiveDefaultQuality.fromQn(defaultLiveQuality)
        Prefs.defaultVideoCodec = enumValueOrDefault(defaultVideoCodec, VideoCodec.HEVC)
        Prefs.playerType = enumValueOrDefault(playerType, PlayerType.Media3)
        Prefs.enableSoftwareVideoDecoder = enableSoftwareVideoDecoder
        Prefs.actionAfterPlay = ActionAfterPlayItems.fromCode(actionAfterPlay)
        Prefs.defaultAudio = Audio.fromCode(defaultAudio)
        Prefs.enableFfmpegAudioRenderer = enableFfmpegAudioRenderer
        Prefs.enableVolumeNormalization = enableVolumeNormalization
        Prefs.defaultDanmakuTypes = defaultDanmakuTypes.mapNotNull { name ->
            runCatching { enumValueOf<DanmakuType>(name) }.getOrNull()
        }
        Prefs.defaultDanmakuScale = defaultDanmakuScale
        Prefs.defaultDanmakuOpacity = defaultDanmakuOpacity
        Prefs.defaultDanmakuSpeedFactor = defaultDanmakuSpeedFactor
        Prefs.defaultDanmakuArea = defaultDanmakuArea
        Prefs.defaultDanmakuMask = defaultDanmakuMask
        Prefs.defaultSubtitleFontSize = defaultSubtitleFontSizeSp.sp
        Prefs.defaultSubtitleBackgroundOpacity = defaultSubtitleBackgroundOpacity
        Prefs.defaultSubtitleBottomPadding = defaultSubtitleBottomPaddingDp.dp
        Prefs.defaultPlaySpeed = PlaySpeedItem.fromCode(defaultPlaySpeed)
        Prefs.seekStepOption = SeekStepOption.fromSeconds(seekStepSeconds)
        Prefs.showVideoInfo = showVideoInfo
        Prefs.showPersistentSeek = showPersistentSeek
        Prefs.showPlayerStats = showPlayerStats
        Prefs.receiveAlphaUpdates = receiveAlphaUpdates
        Prefs.density = density
        Prefs.homeLeftNaviItem = enumValueOrDefault(homeLeftNaviItem, LeftNaviItem.Home)
        Prefs.firstHomeTopNavItem = HomePageSettingItem.fromCode(firstHomeTopNavItem)
        Prefs.firstPersonalTopNavItem =
            enumValueOrDefault(firstPersonalTopNavItem, PersonalTopNavItem.History)
        Prefs.showHotword = showHotword
        Prefs.incognitoMode = incognitoMode
    }

    companion object {
        fun fromPrefs(): SettingsExportData {
            return SettingsExportData(
                apiTypeLegacy = Prefs.apiType.name,
                recommendationApiType = Prefs.recommendationApiType.name,
                enablePersonalizedRecommendation = Prefs.enablePersonalizedRecommendation,
                playbackApiType = Prefs.playbackApiType.name,
                enableProxy = Prefs.enableProxy,
                proxyHttpServer = Prefs.proxyHttpServer,
                proxyGrpcServer = Prefs.proxyGRPCServer,
                preferOfficialCdn = Prefs.preferOfficialCdn,
                defaultQuality = Prefs.defaultQuality.code,
                defaultLiveQuality = Prefs.defaultLiveQuality.qn,
                defaultVideoCodec = Prefs.defaultVideoCodec.name,
                playerType = Prefs.playerType.name,
                enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder,
                actionAfterPlay = Prefs.actionAfterPlay.code,
                defaultAudio = Prefs.defaultAudio.code,
                enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
                enableVolumeNormalization = Prefs.enableVolumeNormalization,
                defaultDanmakuTypes = Prefs.defaultDanmakuTypes.map { it.name },
                defaultDanmakuScale = Prefs.defaultDanmakuScale,
                defaultDanmakuOpacity = Prefs.defaultDanmakuOpacity,
                defaultDanmakuSpeedFactor = Prefs.defaultDanmakuSpeedFactor,
                defaultDanmakuArea = Prefs.defaultDanmakuArea,
                defaultDanmakuMask = Prefs.defaultDanmakuMask,
                defaultSubtitleFontSizeSp = Prefs.defaultSubtitleFontSize.value.toInt(),
                defaultSubtitleBackgroundOpacity = Prefs.defaultSubtitleBackgroundOpacity,
                defaultSubtitleBottomPaddingDp = Prefs.defaultSubtitleBottomPadding.value.toInt(),
                defaultPlaySpeed = Prefs.defaultPlaySpeed.code,
                seekStepSeconds = Prefs.seekStepOption.seconds,
                showVideoInfo = Prefs.showVideoInfo,
                showPersistentSeek = Prefs.showPersistentSeek,
                showPlayerStats = Prefs.showPlayerStats,
                receiveAlphaUpdates = Prefs.receiveAlphaUpdates,
                density = Prefs.density,
                homeLeftNaviItem = Prefs.homeLeftNaviItem.name,
                firstHomeTopNavItem = Prefs.firstHomeTopNavItem.code,
                firstPersonalTopNavItem = Prefs.firstPersonalTopNavItem.name,
                showHotword = Prefs.showHotword,
                incognitoMode = Prefs.incognitoMode
            )
        }

        private inline fun <reified T : Enum<T>> enumValueOrDefault(
            name: String,
            default: T
        ): T {
            return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
        }
    }
}
