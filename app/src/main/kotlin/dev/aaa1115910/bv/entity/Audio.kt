package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R

enum class Audio(val code: Int, private val strRes: Int) {
    A64K(30216, R.string.audio_64k),
    A132K(30232, R.string.audio_132k),
    A192K(30280, R.string.audio_192k),
    ADolbyAtoms(30250, R.string.audio_dolby_atoms),
    AHiRes(30251, R.string.audio_hi_res);

    companion object {
        fun fromCode(code: Int): Audio {
            return when (code) {
                100008 -> A64K
                100009 -> A132K
                100010 -> A192K
                else -> entries.find { it.code == code } ?: A64K
            }
        }
    }

    fun matchesCode(code: Int): Boolean {
        return code == this.code || when (this) {
            A64K -> code == 100008
            A132K -> code == 100009
            A192K -> code == 100010
            ADolbyAtoms,
            AHiRes -> false
        }
    }

    fun getDisplayName(context: Context) = context.getString(strRes)
}
