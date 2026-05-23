package dev.aaa1115910.bv.screen.settings.content

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.settings.LogsActivity
import dev.aaa1115910.bv.component.settings.CookiesDialog
import dev.aaa1115910.bv.component.settings.PrivacyPolicyDialog
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.screen.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.RecommendationApiType

@Composable
fun OtherSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCookiesDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showRecommendationApiDialog by remember { mutableStateOf(false) }
    var showPlaybackApiDialog by remember { mutableStateOf(false) }

    var selectedRecommendationApi by remember { mutableStateOf(Prefs.recommendationApiType) }
    var selectedPlaybackApi by remember { mutableStateOf(Prefs.playbackApiType) }
    var enableCrashReports by remember { mutableStateOf(Prefs.enableCrashReportCollection) }
    var enableUsageStats by remember { mutableStateOf(Prefs.enableAnonymousUsageCollection) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.Other.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingListItem(
            title = "推荐搜索算法",
            supportText = "当前：${selectedRecommendationApi.displayName}；${selectedRecommendationApi.supportText}",
            onClick = { showRecommendationApiDialog = true }
        )
        SettingListItem(
            title = "视频播放接口",
            supportText = "当前：${selectedPlaybackApi.toPlaybackApiDisplayName()}",
            onClick = { showPlaybackApiDialog = true }
        )
        SettingListItem(
            title = "数据导入/导出",
            supportText = "导入或导出登录信息、播放设置、界面设置等软件数据",
            onClick = { showCookiesDialog = true }
        )
        SettingListItem(
            title = "用户协议与隐私政策",
            supportText = "查看数据收集范围、用途、第三方服务、保存期限和退出方式",
            onClick = { showPrivacyPolicyDialog = true }
        )
        SettingSwitchListItem(
            title = "发送崩溃报告",
            supportText = "建议开启；开启后仅发送崩溃和高影响错误的脱敏排障信息",
            checked = enableCrashReports,
            onCheckedChange = {
                enableCrashReports = it
                FirebaseTelemetry.setCrashReportCollectionEnabled(it)
            }
        )
        SettingSwitchListItem(
            title = "发送匿名使用信息",
            supportText = "建议开启；开启后仅发送低频匿名事件，不包含观看内容和搜索词",
            checked = enableUsageStats,
            onCheckedChange = {
                enableUsageStats = it
                FirebaseTelemetry.setAnonymousUsageCollectionEnabled(it)
            }
        )

        SettingListItem(
            title = stringResource(R.string.settings_create_logs_title),
            supportText = stringResource(R.string.settings_create_logs_text),
            onClick = {
                context.startActivity(Intent(context, LogsActivity::class.java))
            }
        )

        if (BuildConfig.DEBUG) {
            SettingListItem(
                title = stringResource(R.string.settings_crash_test_title),
                supportText = stringResource(R.string.settings_crash_test_text),
                onClick = {
                    throw Exception("Boom!")
                }
            )

        }
    }
    CookiesDialog(
        show = showCookiesDialog,
        onHideDialog = { showCookiesDialog = false }
    )
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismissRequest = { showPrivacyPolicyDialog = false })
    }

    if (showRecommendationApiDialog) {
        OptionDialog(
            options = RecommendationApiType.entries.toTypedArray(),
            selectedOption = selectedRecommendationApi,
            onDismiss = { showRecommendationApiDialog = false },
            onSelect = {
                Prefs.recommendationApiType = it
                selectedRecommendationApi = it
            },
            getDisplayName = { it.displayName }
        )
    }

    if (showPlaybackApiDialog) {
        OptionDialog(
            options = ApiType.entries.toTypedArray(),
            selectedOption = selectedPlaybackApi,
            onDismiss = { showPlaybackApiDialog = false },
            onSelect = {
                Prefs.playbackApiType = it
                Prefs.apiType = it
                selectedPlaybackApi = it
            },
            getDisplayName = { it.toPlaybackApiDisplayName() }
        )
    }
}

private fun ApiType.toPlaybackApiDisplayName(): String {
    return when (this) {
        ApiType.Web -> "Http"
        ApiType.App -> "gRPC"
    }
}
