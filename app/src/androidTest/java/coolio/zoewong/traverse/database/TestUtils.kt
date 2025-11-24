package coolio.zoewong.traverse.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TemporaryFolder

/**
 * Creates a temporary database that is closed when the runnable function completes.
 * Based on: https://developer.android.com/training/data-storage/room/testing-db
 */
suspend fun withTemporaryDatabase(tempdir: TemporaryFolder, runnable: suspend (TraverseRepository, TraverseDatabase) -> Unit) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(
        context,
        TraverseDatabase::class.java,
    ).build()

    val mediaDir = tempdir.newFolder()
    val dbMedia = TraverseMedia.createInstance(mediaDir.toPath())

    val repo = TraverseRepository(database, dbMedia)

    runnable(repo, database)
    database.close()
}

/**
 * Resets the IDs of the given MemoryEntities to 0.
 */
fun List<MemoryEntity>.messages(): List<String> {
    return this.map { it.contents }
}


/**
 * Resets the IDs of the given MemoryEntities to 0.
 */
fun List<MemoryEntity>.resetIDs(): List<MemoryEntity> {
    return this.map { it.copy(id = 0) }
}
