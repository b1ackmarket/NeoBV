package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig

internal fun selectUpdateApkAssetName(
    assetNames: List<String>,
    isDebugBuild: Boolean = BuildConfig.DEBUG
): String? {
    val apkNames = assetNames.filter { it.endsWith(".apk") }
    return when {
        isDebugBuild -> apkNames.firstOrNull { it.contains("release", ignoreCase = true) }
            ?: apkNames.firstOrNull { it.contains(".debug", ignoreCase = true) }
            ?: apkNames.firstOrNull { it.contains("debug", ignoreCase = true) }

        else -> apkNames.firstOrNull { it.contains("release", ignoreCase = true) }
            ?: apkNames.firstOrNull { it.contains("alpha", ignoreCase = true) }
            ?: apkNames.firstOrNull()
    }
}

internal fun parseUpdateApkRevision(assetName: String): Int? =
    assetName.substringAfter('_', missingDelimiterValue = "")
        .substringBefore('_', missingDelimiterValue = "")
        .toIntOrNull()
