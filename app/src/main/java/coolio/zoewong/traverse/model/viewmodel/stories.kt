package coolio.zoewong.traverse.model.viewmodel

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.database.StoryEntity
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.model.toDatabase
import coolio.zoewong.traverse.model.toModel
import coolio.zoewong.traverse.ui.state.DatabaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gets all the stories from the database.
 *
 * The returned Story models do not contain the associated memories.
 * Use storyWithMemories to get them.
 */
@Composable
fun getStories(): Pair<Boolean, List<Story>> {
    val context = LocalContext.current
    val dbstate = DatabaseState.current

    var loaded by remember { mutableStateOf(false) }
    var stories by remember {
        mutableStateOf(listOf<Story>())
    }

    dbstate.whenReady { db ->
        LaunchedEffect(db) {
            db.stories.watchAll().collect { entities ->
                stories = entities.map { it.toModel(emptyList()) }
                loaded = true
            }
        }
    }

    return loaded to stories
}

/**
 * Gets a specific story from the database.
 *
 * If the story does not exist, (true, null) will be returned.
 *
 * The returned Story model does not contain the associated memories.
 * Use storyWithMemories to get them.
 */
@Composable
fun getStoryById(id: Long): Pair<Boolean, Story?> {
    val context = LocalContext.current
    val dbstate = DatabaseState.current

    var loaded by remember { mutableStateOf(false) }
    var story by remember {
        mutableStateOf(null as Story?)
    }

    dbstate.whenReady { db ->
        LaunchedEffect(db) {
            story = db.stories.get(id)?.toModel()
            loaded = true
        }
    }

    return loaded to story
}

/**
 * Gets a story's memories from the database, returning
 * a copy of the Story model with its memories populated.
 */
@Composable
fun storyWithMemories(story: Story): Pair<Boolean, Story> {
    val context = LocalContext.current
    val dbstate = DatabaseState.current

    var loaded by remember { mutableStateOf(false) }
    var storyWithMemories by remember {
        mutableStateOf(story.copy())
    }

    dbstate.whenReady { db ->
        LaunchedEffect(db, story) {
            db.stories.watchMemoriesOf(story.toDatabase()).collect {
                val memories = it.map { it.toModel() }
                storyWithMemories = story.copy(memories = memories)
                loaded = true
            }
        }
    }

    return loaded to storyWithMemories
}

/**
 * Creates a function that when called, inserts the given story into the database.
 */
@Composable
fun newEffectToCreateStory(): (story: Story) -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun(story: Story) {
            CoroutineScope(Dispatchers.IO).launch {
                dbstate.waitForReady().stories.insert(
                    story.toDatabase()
                )
            }
        }
    }
}

/**
 * Creates a function that when called, adds the given memory to the given story and
 * updates the database.
 */
@Composable
fun newEffectToAddMemoryToStory(): (story: Story, memory: Memory) -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun(story: Story, memory: Memory) {
            CoroutineScope(Dispatchers.IO).launch {
                dbstate.waitForReady().stories.addMemory(
                    story.toDatabase(),
                    memory.toDatabase(),
                )
            }
        }
    }
}

/**
 * Creates a function that when called, removes the given memory from the given story and
 * updates the database.
 */
@Composable
fun newEffectToRemoveMemoryToStory(): (story: Story, memory: Memory) -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun(story: Story, memory: Memory) {
            CoroutineScope(Dispatchers.IO).launch {
                dbstate.waitForReady().stories.removeMemory(
                    story.toDatabase(),
                    memory.toDatabase(),
                )
            }
        }
    }
}
