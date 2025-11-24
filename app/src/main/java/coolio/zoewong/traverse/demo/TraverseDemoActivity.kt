@file:OptIn(ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coolio.zoewong.traverse.database.AUTOMATICALLY_GENERATED_ID
import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.database.StorySegmentEntity
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.model.viewmodel.getMemories
import coolio.zoewong.traverse.model.viewmodel.newEffectToCreateMemory
import coolio.zoewong.traverse.ui.demo.AppShell
import coolio.zoewong.traverse.ui.demo.JournalScreen
import coolio.zoewong.traverse.ui.demo.SegmentEditorScreen
import coolio.zoewong.traverse.ui.demo.StoryDetailScreen
import coolio.zoewong.traverse.ui.demo.StoryListScreen
import coolio.zoewong.traverse.ui.demo.CreateStoryScreen
import coolio.zoewong.traverse.ui.demo.SettingsScreen
import coolio.zoewong.traverse.ui.demo.MapScreen
import coolio.zoewong.traverse.ui.state.AppState
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.theme.ThemeManager
import coolio.zoewong.traverse.ui.theme.TraverseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class TraverseDemoActivity : ComponentActivity() {

    val idGen = AtomicLong(1)
    private val stories = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme(this)

        if (stories.isEmpty()) {
            fun seed(title: String, location: String?): Story {
                val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
                s.memories += Memory(
                    idGen.getAndDecrement(),
                    timestampMillis = System.currentTimeMillis(),
                    type = Memory.Type.TEXT,
                    text = "Walked from Pacific Center... French Toast!!!"
                )
                s.memories += Memory(
                    idGen.getAndIncrement(),
                    timestampMillis = System.currentTimeMillis(),
                    type = Memory.Type.TEXT,
                    text = "Later... doughnuts. Delicious!"
                )
                return s
            }
            stories += seed("Sad Day! Mish Mish Gone", null)
            stories += seed("Passed Driving Test!", null)
            stories += seed("Stanley Park Vancouver", "Vancouver")
            stories += seed("Stanley Park Vancouver", "Vancouver")
            stories += seed("Stanley Park Vancouver", "Vancouver")
        }

        setContent {
            TraverseTheme{
                val nav = rememberNavController()
                var currentTitle by remember { mutableStateOf("Journal") }
                var currentSubtitle by remember { mutableStateOf<String?>(null) }
                var customNavigationIcon by remember {
                    mutableStateOf<(@Composable () -> Unit)?>(
                        null
                    )
                }
                var customActions by remember {
                    mutableStateOf<(@Composable RowScope.() -> Unit)?>(
                        null
                    )
                }

                AppState {
                    AppShell(
                        nav = nav,
                        currentTitle = currentTitle,
                        subtitle = currentSubtitle,
                        navigationIcon = customNavigationIcon,
                        actions = customActions
                    ) {
                        NavHost(navController = nav, startDestination = "journal") {

                            composable("journal") {
                                currentTitle = "Journal"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null

                                val createMemory = newEffectToCreateMemory()

                                val context = LocalContext.current
                                val dbstate = DatabaseState.current
                                val (loaded, memories) = getMemories()

                                if (!loaded) {
                                    Surface { Text("Loading...") }
                                    return@composable
                                }

                                JournalScreen(
                                    memories = memories,
                                    stories = stories,
                                    onSend = { text, uri ->
                                        createMemory(
                                            Memory(
                                                id = AUTOMATICALLY_GENERATED_ID,
                                                timestampMillis = Calendar.getInstance().time.time,
                                                text = text ?: "",
                                                imageUri = uri?.toString(),
                                                type = when {
                                                    text != null -> Memory.Type.TEXT
                                                    uri != null -> Memory.Type.IMAGE
                                                    else -> throw IllegalArgumentException("No message or image?")
                                                },
                                            )
                                        )
                                    },
                                    onAddToStory = { memory, story ->

                                        CoroutineScope(Dispatchers.IO).launch {
                                            val repo = dbstate.waitForReady()


                                            repo.insertStorySegment(
                                                StorySegmentEntity(
                                                    storyId = story.id,
                                                    memoryId = memory.id,
                                                    text = memory.text,
                                                    imageUri = memory.imageUri,
                                                    createdAt = memory.timestampMillis,
                                                )
                                            )


                                            story.memories.add(0, memory)
                                        }
                                    }
                                )
                            }


                            composable("list") {
                                currentTitle = "My Stories"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null
                                StoryListScreen(
                                    stories = stories,
                                    onOpen = { id -> nav.navigate("detail/$id") },
                                    onCreate = { nav.navigate("create") }
                                )
                            }
                            composable("map") {
                                currentTitle = "Map"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null
                                MapScreen()
                            }

                            composable("create") {
                                currentTitle = "Create Story"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null
                                CreateStoryScreen(
                                    onCancel = { nav.popBackStack() },
                                    onCreate = { title, location ->
                                        val newStory = Story(
                                            id = idGen.getAndIncrement(),
                                            title = title,
                                            dateMillis = System.currentTimeMillis(),
                                            location = location
                                        )
                                        stories.add(0, newStory)
                                        nav.navigate("detail/${newStory.id}") {
                                            popUpTo("list") { inclusive = false }
                                        }
                                    }
                                )
                            }


                            composable("settings") {
                                currentTitle = "Settings"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null
                                SettingsScreen(
                                    onBack = { nav.popBackStack() }
                                )
                            }

                            composable(
                                route = "detail/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.LongType })
                            ) { backStack ->
                                val id = backStack.arguments!!.getLong("id")
                                val story = stories.first { it.id == id }

                                currentTitle = story.title
                                currentSubtitle = SimpleDateFormat("MMMM d'th', yyyy", Locale.getDefault())
                                    .format(Date(story.dateMillis))
                                customNavigationIcon = {
                                    IconButton(onClick = { nav.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                                customActions = {
                                    IconButton(onClick = { /* TODO: Location action */ }) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = "Location"
                                        )
                                    }
                                    IconButton(onClick = { /* TODO: Menu action */ }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "More options"
                                        )
                                    }
                                }

                                StoryDetailScreen(
                                    story = story,
                                    onBack = { nav.popBackStack() },
                                    onAddToStory = { nav.navigate("add/$id") }
                                )
                            }

                            composable(
                                route = "add/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.LongType })
                            ) { backStack ->
                                val id = backStack.arguments!!.getLong("id")
                                val story = stories.first { it.id == id }
                                SegmentEditorScreen(
                                    onCancel = { nav.popBackStack() },
                                    onSubmit = { text, uri: Uri? ->
                                        story.memories.add(
                                            0,
                                            Memory(
                                                idGen.getAndIncrement(),
                                                timestampMillis = System.currentTimeMillis(),
                                                type = Memory.Type.TEXT,
                                                text = text,
                                                imageUri = uri?.toString(),
                                            )
                                        )
                                        nav.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
