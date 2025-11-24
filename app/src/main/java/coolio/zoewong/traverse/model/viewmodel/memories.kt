package coolio.zoewong.traverse.model.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

suspend fun toCreateMemory(db: TraverseRepository, memory: Memory, context: Context) {
    val memoryToSave = if (memory.type == Memory.Type.IMAGE && memory.imageUri != null) {
        withContext(Dispatchers.IO) {
            val sourceUri = Uri.parse(memory.imageUri)
            
            val savedImageUri = try {
                db.media.saveImage(context, sourceUri)
            } catch (e: Exception) {
                Log.e("MemoryLocation", "Failed to save image", e)
                sourceUri
            }
            
            memory.copy(
                imageUri = savedImageUri.toString()
            )
        }
    } else {
        memory
    }
    
    db.memories.insert(memoryToSave.toDatabase())
}
