package dev.aaa1115910.bv.network

import kotlin.test.Test
import kotlin.test.assertEquals

class GithubApiTest {
    @Test
    fun `release repository points to NeoBV`() {
        assertEquals("b1ackmarket", GithubRepositoryConfig.OWNER)
        assertEquals("NeoBV", GithubRepositoryConfig.REPO)
    }
}
