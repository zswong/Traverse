package coolio.zoewong.traverse.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The database holding Traverse's data.
 *
 * When accessing the data, use TraverseRepository instead.
 */
@Database(
    version = 8,
    entities = [
        MemoryEntity::class,
        StoryEntity::class,
        StoryMemoryAssociation::class,
        StoryAnalysisEntity::class,
    ]
)
abstract class TraverseDatabase : RoomDatabase() {
    abstract val memories: MemoryAccess
    abstract val stories: StoryAccess
    abstract val assocStoriesMemories: StoryMemoryAssociationAccess
    abstract val storyAnalysis: StoryAnalysisAccess

    companion object {

        /**
         * Singleton instance of the database.
         */
        @Volatile
        private var instance: TraverseDatabase? = null

        /**
         * Returns the existing TraverseDatabase instance, or creates a new one if the database
         * hasn't been initialized yet.
         */
        internal fun getInstance(context: Context): TraverseDatabase {
            synchronized(this) {
                var instance = instance
                if (instance == null) {
                    instance = createInstance(context)
                    Companion.instance = instance
                }
                return instance
            }
        }

        private fun createInstance(context: Context): TraverseDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TraverseDatabase::class.java,
                "traverse_database"
            )
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}
