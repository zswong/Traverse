package coolio.zoewong.traverse.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters

/**
 * Database entity representing an association between a StoryEntity and MemoryEntity.
 */
@Entity(
    tableName = "story_memory_associations",
    primaryKeys = ["story_id", "memory_id"],
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@TypeConverters(Converters::class)
data class StoryMemoryAssociation(

    /**
     * The ID of the story.
     */
    @ColumnInfo(name = "story_id")
    val storyId: Long,

    /**
     * The ID of the memory.
     */
    @ColumnInfo(name = "memory_id")
    val memoryId: Long,

)
