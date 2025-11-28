package coolio.zoewong.traverse.model

import coolio.zoewong.traverse.database.StoryEntity

typealias Story = StoryEntity

// Compatibility extension properties.

@Deprecated("Use timestamp field instead")
val Story.dateMillis: Long
    get() = this.timestamp
