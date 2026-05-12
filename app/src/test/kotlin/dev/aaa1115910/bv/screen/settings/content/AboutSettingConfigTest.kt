package dev.aaa1115910.bv.screen.settings.content

import kotlin.test.Test
import kotlin.test.assertEquals

class AboutSettingConfigTest {
    @Test
    fun `repository url points to b1ackmarket fork`() {
        assertEquals("https://github.com/b1ackmarket/NeoBV", AboutSettingConfig.repositoryUrl)
    }

    @Test
    fun `repository url uses upstream bottom alignment`() {
        assertEquals(0, AboutSettingConfig.repositoryBottomPaddingDp)
    }
}
