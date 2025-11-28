package coolio.zoewong.traverse.ui.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import coolio.zoewong.traverse.model.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.model.toDatabase
import coolio.zoewong.traverse.model.toModel
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.DatabaseStateAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Provides access to memories.
 */
@Composable
fun MemoryListProvider(
    onReady: (() -> Unit)? = null,
    children: @Composable () -> Unit,
) {
    val database = DatabaseState.current

    var memories by remember {
        mutableStateOf(emptyList<Memory>())
    }

    val manager = remember { MemoryListManager(database) }
    manager.databaseState = database // Update if DatabaseState changes

    // Load memories in the background.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = database.waitForReady()
            var loaded = false
            db.memories.watchAll().collect { newMemories ->
                memories = newMemories.map { it.toModel() }

                if (!loaded) {
                    @Suppress("AssignedValueIsNeverRead") // false positive
                    loaded = true
                    onReady?.invoke()
                }
            }
        }
    }

    // Provide access to children.
    CompositionLocalProvider(localMemoryList provides memories) {
        CompositionLocalProvider(localMemoryListManager provides manager) {
            children()
        }
    }
}

class MemoryListManager(internal var databaseState: DatabaseStateAccessor) {
    fun fromCallback(block: suspend MemoryListManager.(db: TraverseRepository) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = databaseState.waitForReady()
            block(db)
        }
    }

    /**
     * Adds the given Memory to the database.
     */
    suspend fun createMemory(memory: Memory): Memory {
        return databaseState.waitForReady().let {
            val entity = it.memories.insert(memory.toDatabase())
            entity.toModel()
        }
    }
}

/**
 * Returns the list of memories loaded by the nearest MemoryListProvider.
 */
@Composable
fun getMemories(): List<Memory> {
    return localMemoryList.current
}

/**
 * Returns the MemoryListManager associated with the nearest MemoryListProvider.
 */
@Composable
fun getMemoriesManager(): MemoryListManager {
    return localMemoryListManager.current
}

internal val localMemoryListManager = compositionLocalOf<MemoryListManager>(
    policy = referentialEqualityPolicy(), // Faster than checking structural equality
    defaultFactory = throwWhenNotInHierarchy("MemoryListProvider")
)
internal val localMemoryList = compositionLocalOf<List<Memory>>(
    policy = referentialEqualityPolicy(), // Faster than checking structural equality
    defaultFactory = throwWhenNotInHierarchy("MemoryListProvider")
)
