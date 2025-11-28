package coolio.zoewong.traverse.ui.provider

fun <T> throwWhenNotInHierarchy(name: String): () -> T {
    return {
        throw IllegalStateException("$name not in UI hierarchy")
    }
}
