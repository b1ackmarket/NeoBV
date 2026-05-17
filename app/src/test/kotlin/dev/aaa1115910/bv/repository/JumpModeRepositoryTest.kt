package dev.aaa1115910.bv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JumpModeRepositoryTest {
    @Test
    fun `consume returns usable queue for selected aid once`() {
        val repository = JumpModeRepository()
        repository.setPendingQueue(
            source = JumpModeSource.Home,
            selectedAid = 100L,
            items = listOf(
                JumpModeQueueItem(aid = 100L, title = "One"),
                JumpModeQueueItem(aid = 200L, title = "Two")
            )
        )

        val queue = repository.consumeQueueFor(100L)

        assertEquals(JumpModeSource.Home, queue?.source)
        assertEquals(0, queue?.selectedIndex)
        assertNull(repository.consumeQueueFor(100L))
    }

    @Test
    fun `single item queue is ignored`() {
        val repository = JumpModeRepository()
        repository.setPendingQueue(
            source = JumpModeSource.Search,
            selectedAid = 100L,
            items = listOf(JumpModeQueueItem(aid = 100L, title = "Only"))
        )

        assertNull(repository.consumeQueueFor(100L))
    }

    @Test
    fun `wrong selected aid does not leak queue`() {
        val repository = JumpModeRepository()
        repository.setPendingQueue(
            source = JumpModeSource.Personal,
            selectedAid = 100L,
            items = listOf(
                JumpModeQueueItem(aid = 100L, title = "One"),
                JumpModeQueueItem(aid = 200L, title = "Two")
            )
        )

        assertNull(repository.consumeQueueFor(200L))
        assertNull(repository.consumeQueueFor(100L))
    }
}
