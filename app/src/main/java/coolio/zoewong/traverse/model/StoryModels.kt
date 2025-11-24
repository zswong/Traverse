package coolio.zoewong.traverse.model

data class OldStory(
    val id: Long,
    val title: String,
    val dateMillis: Long,
    val location: String?,
    val coverUri: String? = null,
    val memories: MutableList<Memory> = mutableListOf()
)
