@file:Suppress("SpellCheckingInspection", "UNCHECKED_CAST")

package dev.aaa1115910.bv.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.util.generateBuvid
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.component.HomePageSettingItem
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.LiveDanmakuSourceMode
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.live.LiveDefaultQuality
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.screen.settings.content.ActionAfterPlayItems
import dev.aaa1115910.bv.subtitle.translation.DefaultSubtitleTranslationPrompt
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationProviderType
import dev.aaa1115910.bv.viewmodel.player.SeekStepOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Prefs {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val flowMap = ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>()

    /**
     * 基本类型委托 (String, Int, Boolean, Float, Long)
     */
    private fun <T> pref(key: Preferences.Key<T>, default: T): PrefDelegate<T, T> {
        return PrefDelegate(key, default, flowMap)
    }

    /**
     * 对象映射委托 (Enum, Date, Dp, etc.)
     * @param save 转换成基本类型存入 DataStore
     * @param restore 从 DataStore 的基本类型还原为对象
     */
    private fun <T, P> pref(
        key: Preferences.Key<P>,
        default: T,
        save: (T) -> P,
        restore: (P) -> T
    ): PrefDelegate<T, P> {
        return PrefDelegate(key, default, flowMap, save, restore)
    }

    // =========================================================================
    // 账号 & 认证
    // =========================================================================

    var isLogin by pref(PrefKeys.prefIsLoginKey, false)
    var uid by pref(PrefKeys.prefUidKey, 0L)
    var sid by pref(PrefKeys.prefSidKey, "")
    var sessData by pref(PrefKeys.prefSessDataKey, "")
    var biliJct by pref(PrefKeys.prefBiliJctKey, "")
    var uidCkMd5 by pref(PrefKeys.prefUidCkMd5Key, "")
    var tokenExpiredData by pref(
        PrefKeys.prefTokenExpiredDateKey,
        Date(0),
        save = { it.time },
        restore = { Date(it) }
    )
    var accessToken by pref(PrefKeys.prefAccessTokenKey, "")
    var refreshToken by pref(PrefKeys.prefRefreshTokenKey, "")
    var buvid by pref(PrefKeys.prefBuvidKey, "")
    var buvid3 by pref(PrefKeys.prefBuvid3Key, "")

    // =========================================================================
    // 网络 & API
    // =========================================================================

    var apiType by pref(
        PrefKeys.prefApiTypeKey,
        ApiType.Web,
        save = { it.ordinal },
        restore = { ApiType.entries.getOrElse(it) { ApiType.Web } }
    )
    var recommendationApiType by pref(
        PrefKeys.prefRecommendationApiTypeKey,
        RecommendationApiType.App,
        save = { it.ordinal },
        restore = {
            RecommendationApiType.entries
                .getOrElse(it) { RecommendationApiType.App }
        }
    )
    var playbackApiType by pref(
        PrefKeys.prefPlaybackApiTypeKey,
        ApiType.Web,
        save = { it.ordinal },
        restore = { ApiType.entries.getOrElse(it) { ApiType.Web } }
    )
    var enableProxy by pref(PrefKeys.prefEnableProxyKey, false)
    var proxyHttpServer by pref(PrefKeys.prefProxyHttpServerKey, "")
    var proxyGRPCServer by pref(PrefKeys.prefProxyGRPCServerKey, "")
    var preferOfficialCdn by pref(PrefKeys.prefPreferOfficialCdn, PrefDefaultValues.preferOfficialCdn)
    var enablePersonalizedRecommendation by pref(PrefKeys.prefEnablePersonalizedRecommendationKey, true)

    // =========================================================================
    // 播放器 - 视频
    // =========================================================================

    var defaultQuality by pref(
        PrefKeys.prefDefaultQualityKey,
        PrefDefaultValues.defaultQuality,
        save = { it.code },
        restore = { Resolution.fromCode(it) }
    )
    var defaultLiveQuality by pref(
        PrefKeys.prefDefaultLiveQualityKey,
        PrefDefaultValues.defaultLiveQuality,
        save = { it.qn },
        restore = { LiveDefaultQuality.fromQn(it) }
    )
    var defaultVideoCodec by pref(
        PrefKeys.prefDefaultVideoCodecKey,
        PrefDefaultValues.defaultVideoCodec,
        save = { it.ordinal },
        restore = { VideoCodec.fromCode(it) }
    )
    var playerType by pref(
        PrefKeys.prefPlayerTypeKey,
        PlayerType.Media3,
        save = { it.ordinal },
        restore = { PlayerType.entries.getOrElse(it) { PlayerType.Media3 } }
    )
    var enableSoftwareVideoDecoder by pref(PrefKeys.prefEnableSoftwareVideoDecoder, false)
    var actionAfterPlay by pref(
        PrefKeys.prefActionAfterPlayKey,
        ActionAfterPlayItems.AutoNextOrRelated,
        save = { it.code },
        restore = { ActionAfterPlayItems.fromCode(it) }
    )

    // =========================================================================
    // 播放器 - 音频
    // =========================================================================

    var defaultAudio by pref(
        PrefKeys.prefDefaultAudioKey,
        PrefDefaultValues.defaultAudio,
        save = { it.code },
        restore = { Audio.fromCode(it) }
    )
    var enableFfmpegAudioRenderer by pref(
        PrefKeys.prefEnableFfmpegAudioRenderer,
        PrefDefaultValues.enableFfmpegAudioRenderer
    )
    var enableVolumeNormalization by pref(PrefKeys.prefEnableVolumeNormalizationKey, false)

    // =========================================================================
    // 播放器 - 弹幕
    // =========================================================================

    var defaultDanmakuTypes by pref(
        PrefKeys.prefDefaultDanmakuTypesKey,
        listOf(
            DanmakuType.All,
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom
        ),
        save = { list -> list.map { it.ordinal }.joinToString(",") },
        restore = { str ->
            if (str.isEmpty()) emptyList()
            else str.split(",")
                .mapNotNull { runCatching { DanmakuType.entries[it.toInt()] }.getOrNull() }
        }
    )
    var defaultDanmakuEnabled by pref(PrefKeys.prefDefaultDanmakuEnabledKey, true)
    var defaultLiveDanmakuTypes by pref(
        PrefKeys.prefDefaultLiveDanmakuTypesKey,
        listOf(
            DanmakuType.All,
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom
        ),
        save = { list -> list.map { it.ordinal }.joinToString(",") },
        restore = { str ->
            if (str.isEmpty()) emptyList()
            else str.split(",")
                .mapNotNull { runCatching { DanmakuType.entries[it.toInt()] }.getOrNull() }
        }
    )
    var defaultLiveDanmakuEnabled by pref(PrefKeys.prefDefaultLiveDanmakuEnabledKey, true)
    var defaultDanmakuScale by pref(PrefKeys.prefDefaultDanmakuScaleKey, 1.5f)
    var defaultDanmakuOpacity by pref(PrefKeys.prefDefaultDanmakuOpacityKey, 0.5f)
    var defaultDanmakuSpeedFactor by pref(PrefKeys.prefDefaultDanmakuSpeedFactorKey, 1f)
    var defaultDanmakuArea by pref(PrefKeys.prefDefaultDanmakuAreaKey, 0.5f)
    var defaultDanmakuMask by pref(PrefKeys.prefDefaultDanmakuMask, false)
    var defaultLiveDanmakuSourceMode by pref(
        PrefKeys.prefDefaultLiveDanmakuSourceModeKey,
        PrefDefaultValues.defaultLiveDanmakuSourceMode,
        save = { it.ordinal },
        restore = { value ->
            LiveDanmakuSourceMode.entries.getOrElse(value) {
                PrefDefaultValues.defaultLiveDanmakuSourceMode
            }
        }
    )

    // =========================================================================
    // 播放器 - 字幕
    // =========================================================================

    var defaultSubtitleFontSize by pref(
        PrefKeys.prefDefaultSubtitleFontSizeKey,
        24.sp,
        save = { it.value.roundToInt() },
        restore = { it.sp }
    )
    var defaultSubtitleBackgroundOpacity by pref(
        PrefKeys.prefDefaultSubtitleBackgroundOpacityKey,
        0.4f
    )
    var defaultSubtitleBottomPadding by pref(
        PrefKeys.prefDefaultSubtitleBottomPaddingKey,
        12.dp,
        save = { it.value.roundToInt() },
        restore = { it.dp }
    )
    var enableBilingualSubtitle by pref(PrefKeys.prefEnableBilingualSubtitleKey, false)
    var preferBilingualSubtitleOnOsd by pref(PrefKeys.prefPreferBilingualSubtitleOnOsdKey, false)
    var preferCustomSecondarySubtitle by pref(PrefKeys.prefPreferCustomSecondarySubtitleKey, false)
    var subtitleTranslationProviderType by pref(
        PrefKeys.prefSubtitleTranslationProviderTypeKey,
        SubtitleTranslationProviderType.OpenAiCompatible,
        save = { it.ordinal },
        restore = { SubtitleTranslationProviderType.entries.getOrElse(it) { SubtitleTranslationProviderType.OpenAiCompatible } }
    )
    var subtitleTranslationTargetLanguage by pref(PrefKeys.prefSubtitleTranslationTargetLanguageKey, "en")
    var bilingualSubtitleBaseUrl by pref(PrefKeys.prefBilingualSubtitleBaseUrlKey, "")
    var bilingualSubtitleApiKey by pref(PrefKeys.prefBilingualSubtitleApiKeyKey, "")
    var bilingualSubtitleModel by pref(PrefKeys.prefBilingualSubtitleModelKey, "")
    var bilingualSubtitlePrompt by pref(
        PrefKeys.prefBilingualSubtitlePromptKey,
        DefaultSubtitleTranslationPrompt
    )
    var bilingualSubtitleForceAiSubtitle by pref(PrefKeys.prefBilingualSubtitleForceAiSubtitleKey, false)
    var bilingualSubtitleContextBefore by pref(PrefKeys.prefBilingualSubtitleContextBeforeKey, 2)
    var bilingualSubtitleContextAfter by pref(PrefKeys.prefBilingualSubtitleContextAfterKey, 1)
    var bilingualSubtitleRequestBatchSize by pref(PrefKeys.prefBilingualSubtitleRequestBatchSizeKey, 12)
    var subtitleTranslationPreTranslateSeconds by pref(PrefKeys.prefSubtitleTranslationPreTranslateSecondsKey, 90)
    var subtitleTranslationBaiduAppId by pref(PrefKeys.prefSubtitleTranslationBaiduAppIdKey, "")
    var subtitleTranslationBaiduAppKey by pref(PrefKeys.prefSubtitleTranslationBaiduAppKeyKey, "")
    var subtitleTranslationMicrosoftKey by pref(PrefKeys.prefSubtitleTranslationMicrosoftKeyKey, "")
    var subtitleTranslationMicrosoftRegion by pref(PrefKeys.prefSubtitleTranslationMicrosoftRegionKey, "")
    var subtitleTranslationMicrosoftEndpoint by pref(
        PrefKeys.prefSubtitleTranslationMicrosoftEndpointKey,
        "https://api.cognitive.microsofttranslator.com"
    )
    var subtitleTranslationDeepLApiKey by pref(PrefKeys.prefSubtitleTranslationDeepLApiKeyKey, "")
    var subtitleTranslationDeepLEndpoint by pref(
        PrefKeys.prefSubtitleTranslationDeepLEndpointKey,
        "https://api-free.deepl.com"
    )
    var subtitleTranslationDeepLXEndpoint by pref(PrefKeys.prefSubtitleTranslationDeepLXEndpointKey, "")
    var subtitleTranslationDeepLXApiKey by pref(PrefKeys.prefSubtitleTranslationDeepLXApiKeyKey, "")
    var subtitleTranslationVerifiedSignature by pref(PrefKeys.prefSubtitleTranslationVerifiedSignatureKey, "")

    // =========================================================================
    // 播放器 - 界面
    // =========================================================================

    var defaultPlaySpeed by pref(
        PrefKeys.prefDefaultPlaySpeedKey,
        PlaySpeedItem.x1,
        save = { it.code },
        restore = { PlaySpeedItem.fromCode(it) }
    )
    var seekStepOption by pref(
        PrefKeys.prefSeekStepSecondsKey,
        SeekStepOption.Ten,
        save = { it.seconds },
        restore = { SeekStepOption.fromSeconds(it) }
    )
    var showVideoInfo by pref(PrefKeys.prefShowVideoInfoKey, PrefDefaultValues.showVideoInfo)
    var showPersistentSeek by pref(PrefKeys.prefShowPersistentSeekKey, false)
    var showPlayerStats by pref(PrefKeys.prefShowPlayerStatsKey, false)
    var receiveAlphaUpdates by pref(PrefKeys.prefReceiveAlphaUpdatesKey, false)

    // =========================================================================
    // 应用界面
    // =========================================================================

    var density by pref(
        PrefKeys.prefDensityKey,
        resolveDefaultDensity(
            widthPx = BVApp.context.resources.displayMetrics.widthPixels,
            heightPx = BVApp.context.resources.displayMetrics.heightPixels
        )
    )
    val densityFlow = flowMap[PrefKeys.prefDensityKey]!!.asStateFlow() as StateFlow<Float>

    var homeLeftNaviItem by pref(
        PrefKeys.prefHomeLeftNavItem,
        LeftNaviItem.Home,
        save = { it.ordinal },
        restore = { LeftNaviItem.entries.getOrElse(it) { LeftNaviItem.Home } }
    )
    var firstHomeTopNavItem by pref(
        PrefKeys.prefFirstHomeTopNavItemKey,
        HomePageSettingItem.Recommend,
        save = { it.code },
        restore = { HomePageSettingItem.fromCode(it) }
    )
    var firstPersonalTopNavItem by pref(
        PrefKeys.prefFirstPersonalTopNavItemKey,
        PrefDefaultValues.firstPersonalTopNavItem,
        save = { it.ordinal },
        restore = { PersonalTopNavItem.entries.getOrElse(it) { PersonalTopNavItem.ToView } }
    )
    var showHotword by pref(PrefKeys.prefShowHotwordKey, true)
    var enableFocusPreview by pref(PrefKeys.prefEnableFocusPreviewKey, true)
    var enableFocusPreviewMuted by pref(PrefKeys.prefEnableFocusPreviewMutedKey, true)

    // =========================================================================
    // 隐私
    // =========================================================================

    var incognitoMode by pref(PrefKeys.prefIncognitoModeKey, false)
    var hasAcceptedUserAgreement by pref(PrefKeys.prefHasAcceptedUserAgreementKey, false)
    var enableCrashReportCollection by pref(PrefKeys.prefEnableCrashReportCollectionKey, false)
    var enableAnonymousUsageCollection by pref(PrefKeys.prefEnableAnonymousUsageCollectionKey, false)

    // =========================================================================

    /**
     * [必须调用] 在 Application onCreate 中调用此方法。
     * 作用：首先阻塞读取硬盘内DataStore到内存，用于其他模块初始化；
     * 再启动一个长连接监听 DataStore 变化，并自动同步到内存缓存。
     */
    fun init() {
        val initialPrefs = runBlocking {
            BVApp.dataStoreManager.dataStore.data.first()
        }
        updateMemoryCache(initialPrefs)
        checkAndInitBuvid(initialPrefs)

        scope.launch {
            BVApp.dataStoreManager.dataStore.data.collect { preferences ->
                updateMemoryCache(preferences)
            }
        }
    }

    private fun updateMemoryCache(preferences: Preferences) {
        flowMap.forEach { (key, flow) ->
            if (preferences.contains(key)) {
                val newValue = preferences[key]
                flow.value = newValue
            }
        }
    }

    private fun checkAndInitBuvid(prefs: Preferences) {
        if (!prefs.contains(PrefKeys.prefBuvidKey) || prefs[PrefKeys.prefBuvidKey].isNullOrEmpty()) {
            val randomBuvid = generateBuvid()
            buvid = randomBuvid
        }
        if (!prefs.contains(PrefKeys.prefBuvid3Key) || prefs[PrefKeys.prefBuvid3Key].isNullOrEmpty()) {
            val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
            buvid3 = randomBuvid3
        }
    }
}

enum class RecommendationApiType(
    val displayName: String,
    val supportText: String
) {
    Web("Web", "使用网页端推荐和搜索接口"),
    App("App", "使用移动端推荐和搜索接口");

    fun toRequestApiType(): ApiType = when (this) {
        Web -> ApiType.Web
        App -> ApiType.App
    }

    val useAuth: Boolean
        get() = Prefs.enablePersonalizedRecommendation
}

internal object PrefDefaultValues {
    val defaultQuality = Resolution.R8K
    val defaultLiveQuality = LiveDefaultQuality.Dolby
    val defaultVideoCodec = VideoCodec.HEVC
    val defaultAudio = Audio.AHiRes
    const val enableFfmpegAudioRenderer = true
    const val showVideoInfo = true
    const val preferOfficialCdn = true
    val firstPersonalTopNavItem = PersonalTopNavItem.History
    val defaultLiveDanmakuSourceMode = LiveDanmakuSourceMode.HistoryOnly
}

/**
 * 核心委托类：
 * 1. 维护内存缓存 (via MutableStateFlow)
 * 2. Get: 直接读内存 (同步，无锁，极快)
 * 3. Set: 更新内存 + 异步写入 DataStore (不阻塞 UI)
 */
class PrefDelegate<T, P>(
    private val key: Preferences.Key<P>,
    private val defaultValue: T,
    map: ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>,
    private val save: (T) -> P = { it as P },
    private val restore: (P) -> T = { it as T }
) : ReadWriteProperty<Any?, T> {

    private val _flow = MutableStateFlow<Any?>(save(defaultValue))

    init {
        map[key] = _flow
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val rawValue = _flow.value as? P
        return if (rawValue != null) restore(rawValue) else defaultValue
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val persistValue = save(value)

        // 1. 立即更新内存，UI 瞬间响应
        _flow.value = persistValue

        // 2. 异步持久化
        BVApp.dataStoreManager.run {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                editPreference(key, persistValue)
            }
        }
    }
}

private object PrefKeys {
    // 账号 & 认证
    val prefIsLoginKey = booleanPreferencesKey("il")
    val prefUidKey = longPreferencesKey("uid")
    val prefSidKey = stringPreferencesKey("sid")
    val prefSessDataKey = stringPreferencesKey("sd")
    val prefBiliJctKey = stringPreferencesKey("bj")
    val prefUidCkMd5Key = stringPreferencesKey("ucm")
    val prefTokenExpiredDateKey = longPreferencesKey("ted")
    val prefAccessTokenKey = stringPreferencesKey("access_token")
    val prefRefreshTokenKey = stringPreferencesKey("refresh_token")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")

    // 网络 & API
    val prefApiTypeKey = intPreferencesKey("api_type")
    val prefRecommendationApiTypeKey = intPreferencesKey("recommendation_api_type")
    val prefPlaybackApiTypeKey = intPreferencesKey("playback_api_type")
    val prefEnableProxyKey = booleanPreferencesKey("enable_proxy")
    val prefProxyHttpServerKey = stringPreferencesKey("proxy_http_server")
    val prefProxyGRPCServerKey = stringPreferencesKey("proxy_grpc_server")
    val prefPreferOfficialCdn = booleanPreferencesKey("prefer_official_cdn")
    val prefEnablePersonalizedRecommendationKey = booleanPreferencesKey("enable_personalized_recommendation")

    // 播放器 - 视频
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultLiveQualityKey = intPreferencesKey("dlq")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefPlayerTypeKey = intPreferencesKey("pt")
    val prefEnableSoftwareVideoDecoder = booleanPreferencesKey("enable_software_video_decoder")
    val prefActionAfterPlayKey = intPreferencesKey("action_after_play")

    // 播放器 - 音频
    val prefDefaultAudioKey = intPreferencesKey("da")
    val prefEnableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")
    val prefEnableVolumeNormalizationKey = booleanPreferencesKey("enable_volume_normalization")

    // 播放器 - 弹幕
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuEnabledKey = booleanPreferencesKey("default_danmaku_enabled")
    val prefDefaultLiveDanmakuTypesKey = stringPreferencesKey("live_danmaku_types")
    val prefDefaultLiveDanmakuEnabledKey = booleanPreferencesKey("live_danmaku_enabled")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuSpeedFactorKey = floatPreferencesKey("ddsf")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")
    val prefDefaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val prefDefaultLiveDanmakuSourceModeKey = intPreferencesKey("live_danmaku_source_mode")

    // 播放器 - 字幕
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")
    val prefEnableBilingualSubtitleKey = booleanPreferencesKey("enable_bilingual_subtitle")
    val prefPreferBilingualSubtitleOnOsdKey = booleanPreferencesKey("prefer_bilingual_subtitle_on_osd")
    val prefPreferCustomSecondarySubtitleKey = booleanPreferencesKey("prefer_custom_secondary_subtitle")
    val prefSubtitleTranslationProviderTypeKey = intPreferencesKey("subtitle_translation_provider_type")
    val prefSubtitleTranslationTargetLanguageKey = stringPreferencesKey("subtitle_translation_target_language")
    val prefBilingualSubtitleBaseUrlKey = stringPreferencesKey("bilingual_subtitle_base_url")
    val prefBilingualSubtitleApiKeyKey = stringPreferencesKey("bilingual_subtitle_api_key")
    val prefBilingualSubtitleModelKey = stringPreferencesKey("bilingual_subtitle_model")
    val prefBilingualSubtitlePromptKey = stringPreferencesKey("bilingual_subtitle_prompt")
    val prefBilingualSubtitleForceAiSubtitleKey = booleanPreferencesKey("bilingual_subtitle_force_ai")
    val prefBilingualSubtitleContextBeforeKey = intPreferencesKey("bilingual_subtitle_context_before")
    val prefBilingualSubtitleContextAfterKey = intPreferencesKey("bilingual_subtitle_context_after")
    val prefBilingualSubtitleRequestBatchSizeKey = intPreferencesKey("bilingual_subtitle_batch_size")
    val prefSubtitleTranslationPreTranslateSecondsKey = intPreferencesKey("subtitle_translation_pre_translate_seconds")
    val prefSubtitleTranslationBaiduAppIdKey = stringPreferencesKey("subtitle_translation_baidu_app_id")
    val prefSubtitleTranslationBaiduAppKeyKey = stringPreferencesKey("subtitle_translation_baidu_app_key")
    val prefSubtitleTranslationMicrosoftKeyKey = stringPreferencesKey("subtitle_translation_microsoft_key")
    val prefSubtitleTranslationMicrosoftRegionKey = stringPreferencesKey("subtitle_translation_microsoft_region")
    val prefSubtitleTranslationMicrosoftEndpointKey = stringPreferencesKey("subtitle_translation_microsoft_endpoint")
    val prefSubtitleTranslationDeepLApiKeyKey = stringPreferencesKey("subtitle_translation_deepl_api_key")
    val prefSubtitleTranslationDeepLEndpointKey = stringPreferencesKey("subtitle_translation_deepl_endpoint")
    val prefSubtitleTranslationDeepLXEndpointKey = stringPreferencesKey("subtitle_translation_deeplx_endpoint")
    val prefSubtitleTranslationDeepLXApiKeyKey = stringPreferencesKey("subtitle_translation_deeplx_api_key")
    val prefSubtitleTranslationVerifiedSignatureKey = stringPreferencesKey("subtitle_translation_verified_signature")

    // 播放器 - 界面
    val prefDefaultPlaySpeedKey = intPreferencesKey("dps")
    val prefSeekStepSecondsKey = intPreferencesKey("seek_step_seconds")
    val prefShowVideoInfoKey = booleanPreferencesKey("show_video_info")
    val prefShowPersistentSeekKey = booleanPreferencesKey("show_persistent_seek")
    val prefShowPlayerStatsKey = booleanPreferencesKey("show_player_stats")
    val prefReceiveAlphaUpdatesKey = booleanPreferencesKey("receive_alpha_updates")

    // 应用界面
    val prefDensityKey = floatPreferencesKey("density")
    val prefHomeLeftNavItem = intPreferencesKey("home_left_nav")
    val prefFirstHomeTopNavItemKey = intPreferencesKey("first_home_top_nav")
    val prefFirstPersonalTopNavItemKey = intPreferencesKey("first_personal_top_nav")
    val prefShowHotwordKey = booleanPreferencesKey("shw")
    val prefEnableFocusPreviewKey = booleanPreferencesKey("enable_focus_preview")
    val prefEnableFocusPreviewMutedKey = booleanPreferencesKey("enable_focus_preview_muted")

    // 隐身模式
    val prefIncognitoModeKey = booleanPreferencesKey("im")
    val prefHasAcceptedUserAgreementKey = booleanPreferencesKey("has_accepted_user_agreement")
    val prefEnableCrashReportCollectionKey = booleanPreferencesKey("enable_crash_report_collection")
    val prefEnableAnonymousUsageCollectionKey = booleanPreferencesKey("enable_anonymous_usage_collection")
}
