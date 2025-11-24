package coolio.zoewong.traverse.model

import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.database.MemoryEntity
import java.util.Date

/**
 * Model representing a memory.
 */
data class Memory(
    val id: Long,
    val timestampMillis: Long,
    val type: Type,
    val text: String,
    val imageUri: String? = null
) {
    enum class Type {
        TEXT,
        IMAGE;
    }

    val timestampDate get() = Date(timestampMillis)
}

/**
 * Converts the database representation of a memory type into the model representation.
 */
fun MemoryType.toModel(): Memory.Type {
    return when (this) {
        MemoryType.TEXT -> Memory.Type.TEXT
        MemoryType.IMAGE -> Memory.Type.IMAGE
    }
}

/**
 * Converts the model representation of a memory type into the database representation.
 */
fun Memory.Type.toDatabase(): MemoryType {
    return when (this) {
        Memory.Type.TEXT -> MemoryType.TEXT
        Memory.Type.IMAGE -> MemoryType.IMAGE
    }
}

/**
 * Converts the database representation of a memory into the model representation.
 */
fun MemoryEntity.toModel(): Memory {
    return Memory(
        id = this.id,
        timestampMillis = this.timestamp,
        type = this.type.toModel(),
        text = when (this.type) {
            MemoryType.TEXT -> this.contents
            MemoryType.IMAGE -> ""
        },
        imageUri = when (this.type) {
            MemoryType.TEXT -> null
            MemoryType.IMAGE -> this.contents
        }
    )
}

/**
 * Converts the model representation of a memory into the database representation.
 */
fun Memory.toDatabase(): MemoryEntity {
    val contents: String = when (this.type) {
        Memory.Type.TEXT -> this.text
        Memory.Type.IMAGE -> this.imageUri!!
    }

    return MemoryEntity(
        id = this.id,
        type = this.type.toDatabase(),
        timestamp = this.timestampMillis,
        contents = contents
    )
}
