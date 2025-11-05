package coolio.zoewong.traverse.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * Creates a temporary database that is closed when the runnable function completes.
 * Based on: https://developer.android.com/training/data-storage/room/testing-db
 */
suspend fun withTemporaryDatabase(runnable: suspend (TraverseDatabase) -> Unit) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(
        context,
        TraverseDatabase::class.java,
    ).build()

    runnable(database)
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
