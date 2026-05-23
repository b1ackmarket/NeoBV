package dev.aaa1115910.bv.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class ResolutionDisplayNameTest {
    @Test
    fun `1080 plus and 1080p60 keep distinct short display names`() {
        assertEquals(
            "1080P+",
            Resolution.R1080PPlus.code.toVideoQualityDisplayName("1080P") { it.testShortName() }
        )
        assertEquals(
            "1080P60",
            Resolution.R1080P60.code.toVideoQualityDisplayName("1080P") { it.testShortName() }
        )
    }

    @Test
    fun `other qualities keep api description when present`() {
        assertEquals(
            "4K 超清",
            Resolution.R4K.code.toVideoQualityDisplayName("4K 超清") { it.testShortName() }
        )
    }

    private fun Resolution.testShortName(): String = when (this) {
        Resolution.R1080PPlus -> "1080P+"
        Resolution.R1080P60 -> "1080P60"
        Resolution.R4K -> "4K"
        else -> name
    }
}
