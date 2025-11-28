package coolio.zoewong.traverse.ui.provider

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.model.toDatabase
import coolio.zoewong.traverse.model.toModel
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.DatabaseStateAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Provides access to stories.
 */
@Composable
fun StoryListProvider(
    onReady: (() -> Unit)? = null,
    children: @Composable () -> Unit,
) {
    val database = DatabaseState.current

    var stories by remember {
        mutableStateOf(emptyList<Story>())
    }

    val manager = remember { StoryListManager(database) }
    manager.databaseState = database // Update if DatabaseState changes

    // Load stories in the background.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = database.waitForReady()
            var loaded = false
            db.stories.watchAll().collect { newMemories ->
                stories = newMemories.map { it.toModel() }

                if (!loaded) {
                    @Suppress("AssignedValueIsNeverRead") // false positive
                    loaded = true
                    onReady?.invoke()
                }
            }
        }
    }

    // Provide access to children.
    CompositionLocalProvider(localStoryList provides stories) {
        CompositionLocalProvider(localStoryListManager provides manager) {
            children()
        }
    }
}

class StoryListManager(internal var databaseState: DatabaseStateAccessor) {
    fun fromCallback(block: suspend StoryListManager.(db: TraverseRepository) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = databaseState.waitForReady()
            block(db)
        }
    }

    /**
     * Inserts the given story into the database.
     */
    suspend fun createStory(story: Story): Story {
        return databaseState.waitForReady().let {
            it.stories.insert(story.toDatabase()).toModel()
        }
    }

    /**
     * Adds the given memory to the given story.
     */
    suspend fun addMemoryToStory(story: Story, memory: Memory) {
        databaseState.waitForReady().apply {
            try {
                stories.addMemory(
                    story.toDatabase(),
                    memory.toDatabase(),
                )
            } catch (e: SQLiteConstraintException) {
                Log.i("addMemoryToStory", "Memory ${memory.id} already in story ${story.id}")
            }
        }
    }

    /**
     * Removes the given memory from the given story.
     */
    suspend fun removeMemoryFromStory(story: Story, memory: Memory) {
        databaseState.waitForReady().apply {
            stories.removeMemory(
                story.toDatabase(),
                memory.toDatabase(),
            )
        }
    }

    @Composable
    fun loadMemoriesOf(story: Story): Pair<List<Memory>, Boolean> {
        var loaded by remember { mutableStateOf(false) }
        var memories by remember { mutableStateOf(emptyList<Memory>())}

        LaunchedEffect( story) {
            val db = databaseState.waitForReady()
            db.stories.watchMemoriesOf(story.toDatabase()).collect {
                memories = it.map { it.toModel() }
                loaded = true
            }
        }

        return memories to loaded
    }
}

/**
 * Returns the list of stories loaded by the nearest StoryListProvider.
 */
@Composable
fun getStories(): List<Story> {
    return localStoryList.current
}

/**
 * Returns the StoryListManager associated with the nearest StoryListProvider.
 */
@Composable
fun getStoriesManager(): StoryListManager {
    return localStoryListManager.current
}

internal val localStoryList = compositionLocalOf<List<Story>>(
    policy = referentialEqualityPolicy(), // Faster than checking structural equality
    defaultFactory = throwWhenNotInHierarchy("StoryListProvider"),
)

internal val localStoryListManager = compositionLocalOf<StoryListManager>(
    policy = referentialEqualityPolicy(), // Faster than checking structural equality
    defaultFactory = throwWhenNotInHierarchy("StoryListProvider"),
)
