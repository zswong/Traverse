package coolio.zoewong.traverse.database

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.Entity
import androidx.room.PrimaryKey
import coolio.zoewong.traverse.database.MemoryType


/**
 * Database entity representing a memory.
 */
@Entity(
    tableName = "memories",
)
data class MemoryEntity(

    /**
     * The primary key of the memory.
     * This is auto-generated starting from 1.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /**
     * The memory type.
     */
    @ColumnInfo(name = "type")
    val type: MemoryType,

    /**
     * The timestamp of when the memory was created.
     * Recorded as milliseconds since the Unix epoch.
     */
    @ColumnInfo(name = "timestamp", index = true, typeAffinity = INTEGER)
    val timestamp: Long,

    /**
     * The memory contents.
     * May be the memory text or a URL pointing to an image stored on the phone's file system.
     */
    @ColumnInfo(name = "contents")
    val contents: String

)
