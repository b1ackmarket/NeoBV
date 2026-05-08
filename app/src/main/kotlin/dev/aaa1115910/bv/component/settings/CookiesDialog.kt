package dev.aaa1115910.bv.component.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.AuthTransferStorage
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.launch

@Composable
fun CookiesDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { BVApp.koinApplication.koin.get<UserRepository>() }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val authData = AuthTransferStorage.importFromUri(context, uri)
                userRepository.addUser(authData)
                val displayName = AuthTransferStorage.resolveDisplayName(context, uri)
                "已从 $displayName 导入登录信息".toast(context, Toast.LENGTH_LONG)
            }.onFailure {
                println(it.stackTraceToString())
                "导入失败：${it.message ?: "无法解析文件"}".toast(context, Toast.LENGTH_LONG)
            }
        }
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "Cookies 导入/导出") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "导出会默认写入电视共享 Documents 目录，便于在 debug / release 之间转移登录信息。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        val exportResult = AuthTransferStorage.exportToDocuments(
                                            context = context,
                                            authData = AuthData.fromPrefs()
                                        )
                                        "已导出到 ${exportResult.displayPath}".toast(
                                            context,
                                            Toast.LENGTH_LONG
                                        )
                                    }.onFailure {
                                        println(it.stackTraceToString())
                                        "导出失败：${it.message ?: "无法写入文件"}".toast(
                                            context,
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(text = "导出到 Documents")
                        }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf("application/json", "text/plain", "*/*")
                                )
                            }
                        ) {
                            Text(text = "从文件导入")
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { onHideDialog() }) {
                    Text(text = "关闭")
                }
            },
            dismissButton = {}
        )
    }
}
