package dev.aaa1115910.bv.screen.settings.content

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.live.LiveDefaultQuality
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.plugin.impl.sponsorblock.PrefsSponsorBlockConfigStore
import dev.aaa1115910.bv.screen.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.player.SeekStepOption
import kotlinx.coroutines.launch

internal val selectableDefaultVideoQualities: Array<Resolution>
    get() = Resolution.entries
        .filterNot { it == Resolution.R720P60 }
        .toTypedArray()

@Composable
fun AudioVideoSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val sponsorBlockStore = remember { PrefsSponsorBlockConfigStore() }

    var showResolutionDialog by remember { mutableStateOf(false) }
    var showLiveQualityDialog by remember { mutableStateOf(false) }
    var showAudioCodecDialog by remember { mutableStateOf(false) }
    var showVideoCodecDialog by remember { mutableStateOf(false) }
    var showPlaySpeedDialog by remember { mutableStateOf(false) }
    var showSeekStepDialog by remember { mutableStateOf(false) }
    var showActionAfterPlayDialog by remember { mutableStateOf(false) }

    var selectedResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var selectedLiveQuality by remember { mutableStateOf(Prefs.defaultLiveQuality) }
    var selectedVideoCodec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var selectedAudioCodec by remember { mutableStateOf(Prefs.defaultAudio) }
    var selectedPlaySpeed by remember { mutableStateOf(Prefs.defaultPlaySpeed) }
    var selectedSeekStep by remember { mutableStateOf(Prefs.seekStepOption) }
    var selectedActionAfterPlay by remember { mutableStateOf(Prefs.actionAfterPlay) }

    var enableFfmpegAudioRenderer by remember { mutableStateOf(Prefs.enableFfmpegAudioRenderer) }
    var enableVolumeNormalization by remember { mutableStateOf(Prefs.enableVolumeNormalization) }
    var enableSoftwareVideoRenderer by remember { mutableStateOf(Prefs.enableSoftwareVideoDecoder) }
    var enableBilingualSubtitle by remember { mutableStateOf(Prefs.enableBilingualSubtitle) }
    var enableDanmakuFilterWebConfig by remember { mutableStateOf(Prefs.enableDanmakuFilterWebConfig) }
    var sponsorBlockEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sponsorBlockEnabled = sponsorBlockStore.isEnabled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.AudioVideo.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingListItem(
            title = "默认视频画质",
            supportText = "当前：${selectedResolution.getDisplayName(context)}",
            onClick = { showResolutionDialog = true }
        )
        SettingListItem(
            title = "默认直播画质",
            supportText = "当前：${selectedLiveQuality.displayName}",
            onClick = { showLiveQualityDialog = true }
        )
        SettingListItem(
            title = "默认视频编码",
            supportText = "当前：${selectedVideoCodec.getDisplayName(context)}",
            onClick = { showVideoCodecDialog = true }
        )
        SettingListItem(
            title = "默认音频编码",
            supportText = "当前：${selectedAudioCodec.getDisplayName(context)}",
            onClick = { showAudioCodecDialog = true }
        )
        SettingListItem(
            title = "默认播放速度",
            supportText = "当前：${selectedPlaySpeed.getDisplayName(context)}",
            onClick = { showPlaySpeedDialog = true }
        )
        SettingListItem(
            title = "左右键快进/快退步长",
            supportText = "当前：${selectedSeekStep.seconds} 秒",
            onClick = { showSeekStepDialog = true }
        )
        SettingListItem(
            title = "播放结束动作",
            supportText = "当前：${selectedActionAfterPlay.getDisplayName(context)}",
            onClick = { showActionAfterPlayDialog = true }
        )
        SettingSwitchListItem(
            title = stringResource(R.string.settings_media_software_video_renderer_title),
            supportText = stringResource(R.string.settings_media_software_video_renderer_text),
            checked = enableSoftwareVideoRenderer,
            onCheckedChange = {
                enableSoftwareVideoRenderer = it
                Prefs.enableSoftwareVideoDecoder = it
            }
        )
        SettingSwitchListItem(
            title = stringResource(R.string.settings_media_ffmpeg_audio_renderer_title),
            supportText = stringResource(R.string.settings_media_ffmpeg_audio_renderer_text),
            checked = enableFfmpegAudioRenderer,
            onCheckedChange = {
                enableFfmpegAudioRenderer = it
                Prefs.enableFfmpegAudioRenderer = it
            }
        )
        SettingSwitchListItem(
            title = "音量均衡",
            supportText = "尝试压低突出的峰值、抬高偏小的声音，切换后重新进入播放生效",
            checked = enableVolumeNormalization,
            onCheckedChange = {
                enableVolumeNormalization = it
                Prefs.enableVolumeNormalization = it
            }
        )
        SettingSwitchListItem(
            title = "双语字幕",
            supportText = "启用后可在播放器字幕菜单里选择副字幕；翻译配置请到 ${HttpServer.getServerAddress("/subtitle")} 调整",
            checked = enableBilingualSubtitle,
            onCheckedChange = {
                enableBilingualSubtitle = it
                Prefs.enableBilingualSubtitle = it
            }
        )
        SettingSwitchListItem(
            title = "弹幕过滤",
            supportText = "开启后可到 ${HttpServer.getServerAddress("/danmaku")} 调整关键词、正则、用户过滤和 B 站云端规则",
            checked = enableDanmakuFilterWebConfig,
            onCheckedChange = {
                enableDanmakuFilterWebConfig = it
                Prefs.enableDanmakuFilterWebConfig = it
            }
        )
        SettingSwitchListItem(
            title = "空降助手",
            supportText = "默认仅显示提示；分类细项请到 ${HttpServer.getServerAddress("/sponsorblock")} 调整",
            checked = sponsorBlockEnabled,
            onCheckedChange = {
                sponsorBlockEnabled = it
                scope.launch {
                    sponsorBlockStore.setEnabled(it)
                }
            }
        )
    }
    // 弹窗复用组件
    if (showResolutionDialog) {
        OptionDialog(
            options = selectableDefaultVideoQualities,
            selectedOption = selectedResolution,
            onDismiss = { showResolutionDialog = false },
            onSelect = {
                Prefs.defaultQuality = it
                selectedResolution = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showVideoCodecDialog) {
        OptionDialog(
            options = VideoCodec.entries.toTypedArray(),
            selectedOption = selectedVideoCodec,
            onDismiss = { showVideoCodecDialog = false },
            onSelect = {
                Prefs.defaultVideoCodec = it
                selectedVideoCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showLiveQualityDialog) {
        OptionDialog(
            options = LiveDefaultQuality.entries.toTypedArray(),
            selectedOption = selectedLiveQuality,
            onDismiss = { showLiveQualityDialog = false },
            onSelect = {
                Prefs.defaultLiveQuality = it
                selectedLiveQuality = it
            },
            getDisplayName = { it.displayName }
        )
    }

    if (showAudioCodecDialog) {
        OptionDialog(
            options = Audio.entries.toTypedArray(),
            selectedOption = selectedAudioCodec,
            onDismiss = { showAudioCodecDialog = false },
            onSelect = {
                Prefs.defaultAudio = it
                selectedAudioCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showPlaySpeedDialog) {
        OptionDialog(
            options = PlaySpeedItem.entries.toTypedArray(),
            selectedOption = selectedPlaySpeed,
            onDismiss = { showPlaySpeedDialog = false },
            onSelect = {
                Prefs.defaultPlaySpeed = it
                selectedPlaySpeed = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showSeekStepDialog) {
        OptionDialog(
            options = SeekStepOption.entries.toTypedArray(),
            selectedOption = selectedSeekStep,
            onDismiss = { showSeekStepDialog = false },
            onSelect = {
                Prefs.seekStepOption = it
                selectedSeekStep = it
            },
            getDisplayName = { "${it.seconds} 秒" }
        )
    }

    if (showActionAfterPlayDialog) {
        OptionDialog(
            options = ActionAfterPlayItems.entries.toTypedArray(),
            selectedOption = selectedActionAfterPlay,
            onDismiss = { showActionAfterPlayDialog = false },
            onSelect = {
                Prefs.actionAfterPlay = it
                selectedActionAfterPlay = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }
}

enum class ActionAfterPlayItems (val code: Int, private val displayName: String){
    Pause(0, "暂停"),
    AutoNextOrRelated(1, "自动播放下一集或推荐视频"),
    Exit(2, "退出播放器");


    companion object{
        fun fromCode(code: Int): ActionAfterPlayItems {
            return when (code) {
                1, 3, 4 -> AutoNextOrRelated
                else -> ActionAfterPlayItems.entries.find { it.code == code } ?: Exit
            }
        }
    }

    fun getDisplayName(context: Context): String {
        return displayName
    }
}
