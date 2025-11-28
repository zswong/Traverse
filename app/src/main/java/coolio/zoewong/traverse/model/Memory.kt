package coolio.zoewong.traverse.model

import coolio.zoewong.traverse.database.MemoryEntity
import coolio.zoewong.traverse.database.MemoryType

typealias Memory = MemoryEntity

// Compatibility extension properties.

@Deprecated("Use timestamp field instead")
val Memory.timestampMillis: Long
    get() = this.timestamp

@Deprecated("Check type == MemoryType.TEXT and use contents field instead")
val Memory.text: String?
    get() = when (type) {
        MemoryType.TEXT -> this.contents
        MemoryType.IMAGE -> null
    }

@Deprecated("Check type == MemoryType.IMAGE and use contents field instead")
val Memory.imageUri: String?
    get() = when (type) {
        MemoryType.TEXT -> null
        MemoryType.IMAGE -> this.contents
    }
