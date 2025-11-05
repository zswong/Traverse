package coolio.zoewong.traverse.database

/**
 * The type of data that a MemoryEntity holds.
 */
enum class MemoryType {

    /**
     * The memory holds a plain text message.
     */
    TEXT,

    /**
     * The memory references an image.
     * The contents member contains a string-formatted URL pointing to the image on the filesystem.
     */
    IMAGE,
}