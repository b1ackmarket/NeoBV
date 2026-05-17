package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig

private val StrictUpdateApkNamePattern =
    Regex("""^NeoBV_(\d+)_\d+\.\d+\.\d+\.r\d+\.[0-9a-fA-F]{7,40}\.(release|alpha)_default_universal\.apk$""")

internal data class UpdateApkAsset(
    val name: String,
    val revision: Int,
    val type: UpdateReleaseType
)

@Suppress("UNUSED_PARAMETER")
internal fun selectUpdateApkAssetName(
    assetNames: List<String>,
    isDebugBuild: Boolean = BuildConfig.DEBUG,
    type: UpdateReleaseType = UpdateReleaseType.Release
): String? {
    return assetNames
        .mapNotNull(::parseUpdateApkAsset)
        .filter { it.type == type }
        .maxByOrNull { it.revision }
        ?.name
}

internal fun parseUpdateApkRevision(assetName: String): Int? =
    parseUpdateApkAsset(assetName)?.revision

internal fun parseUpdateApkAsset(assetName: String): UpdateApkAsset? {
    val match = StrictUpdateApkNamePattern.matchEntire(assetName) ?: return null
    val revision = match.groupValues[1].toIntOrNull() ?: return null
    val type = when (match.groupValues[2].lowercase()) {
        "release" -> UpdateReleaseType.Release
        "alpha" -> UpdateReleaseType.Alpha
        else -> return null
    }
    return UpdateApkAsset(
        name = assetName,
        revision = revision,
        type = type
    )
}
