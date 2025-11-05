package coolio.zoewong.traverse.model

data class Story(
    val id: Long,
    val title: String,
    val dateMillis: Long,
    val location: String?,
    val coverUri: String? = null,
    val segments: MutableList<Segment> = mutableListOf()
)

data class Segment(
    val id: Long,
    val timestampMillis: Long,
    val text: String,
    val imageUri: String? = null
)
