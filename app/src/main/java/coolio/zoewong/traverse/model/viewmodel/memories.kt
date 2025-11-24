package coolio.zoewong.traverse.model.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.toDatabase
import coolio.zoewong.traverse.model.toModel
import coolio.zoewong.traverse.ui.state.DatabaseState

/**
 * Gets all the memories from the database.
 */
@Composable
fun getMemories(): Pair<Boolean, List<Memory>> {
    val context = LocalContext.current
    val dbstate = DatabaseState.current

    var loaded by remember { mutableStateOf(false) }
    var memories by remember {
        mutableStateOf(listOf<Memory>())
    }

    dbstate.whenReady { db ->
        LaunchedEffect(db) {
            db.memories.watchAll().collect { entities ->
                memories = entities.map { it.toModel() }
                loaded = true
            }
        }
    }

    return loaded to memories
}

/**
 * Creates a function that when called, inserts the given memory into the database.
 */
suspend fun toCreateMemory(db: TraverseRepository, memory: Memory) {
    db.memories.insert(memory.toDatabase())
}
