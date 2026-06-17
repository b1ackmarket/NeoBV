package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R

enum class DanmakuDensity(val strRes: Int, val keepRatio: Float) {
    D20(R.string.danmaku_density_20, 0.20f),
    D40(R.string.danmaku_density_40, 0.40f),
    D60(R.string.danmaku_density_60, 0.60f),
    D80(R.string.danmaku_density_80, 0.80f),
    D100(R.string.danmaku_density_100, 1.00f);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun fromRatio(ratio: Float): DanmakuDensity =
            entries.minByOrNull { Math.abs(it.keepRatio - ratio) } ?: D100

        fun getIndexByRatio(ratio: Float): Int =
            entries.indexOf(fromRatio(ratio)).takeIf { it >= 0 } ?: D100.ordinal
    }
}
