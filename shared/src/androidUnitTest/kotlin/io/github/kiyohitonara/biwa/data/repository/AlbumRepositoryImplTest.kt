package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlbumRepositoryImplTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: AlbumRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Match the production drivers, which enable foreign key enforcement so
        // ON DELETE CASCADE fires for nested album subtrees.
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        BiwaDatabase.Schema.create(driver)
        repository = AlbumRepositoryImpl(driver, Logger(loggerConfigInit()))
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    private suspend fun create(
        id: String,
        name: String,
        parentId: String? = null,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L, parentId = parentId))

    @Test
    fun `createAlbum persists parentId`() =
        runTest {
            create("parent", "Parent")
            create("child", "Child", parentId = "parent")

            assertEquals("parent", repository.getAlbumById("child")?.parentId)
            assertNull(repository.getAlbumById("parent")?.parentId)
        }

    @Test
    fun `moveAlbum updates parentId`() =
        runTest {
            create("parent", "Parent")
            create("child", "Child")

            repository.moveAlbum("child", "parent")
            assertEquals("parent", repository.getAlbumById("child")?.parentId)

            repository.moveAlbum("child", null)
            assertNull(repository.getAlbumById("child")?.parentId)
        }

    @Test
    fun `deleteAlbum cascades to descendant albums`() =
        runTest {
            create("a", "A")
            create("b", "B", parentId = "a")
            create("c", "C", parentId = "b")
            create("d", "D")

            repository.deleteAlbum("a")

            val remaining = repository.getAllAlbums().first().map { it.id }
            assertEquals(listOf("d"), remaining)
            assertTrue(repository.getAlbumById("b") == null && repository.getAlbumById("c") == null)
        }
}
