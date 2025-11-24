package coolio.zoewong.traverse.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing "a memory attached to a story".
 */
@Entity(tableName = "story_segments")
data class StorySegmentEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /**
     * Which story this segment belongs to.
     * We use the in-app Story.id as a foreign key (no FK constraint needed).
     */
    @ColumnInfo(name = "story_id", index = true)
    val storyId: Long,

    /**
     * Optional link back to the MemoryEntity.
     * 以后如果 ChatMsg 直接从 MemoryEntity 映射，可以存这个。
     */
    @ColumnInfo(name = "memory_id", index = true)
    val memoryId: Long? = null,

    /**
     * Text content of this segment (for text memories).
     */
    @ColumnInfo(name = "text")
    val text: String? = null,

    /**
     * URI of the image (for image memories).
     */
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    /**
     * When this segment was created.
     */
    @ColumnInfo(name = "created_at", index = true, typeAffinity = ColumnInfo.INTEGER)
    val createdAt: Long,
)
