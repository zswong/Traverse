@file:OptIn(ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.model.viewmodel.getMemories
import coolio.zoewong.traverse.model.viewmodel.getStories
import coolio.zoewong.traverse.model.viewmodel.getStoryById
import coolio.zoewong.traverse.model.viewmodel.newEffectToAddMemoryToStory
import coolio.zoewong.traverse.model.viewmodel.newEffectToCreateMemory
import coolio.zoewong.traverse.model.viewmodel.newEffectToCreateStory
import coolio.zoewong.traverse.model.viewmodel.storyWithMemories
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme(this)

//        if (stories.isEmpty()) {
//            fun seed(title: String, location: String?): Story {
//                val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
//                // Relational database can't have rows from tables nested inside other tables' rows.
////                s.memories += Memory(
////                    idGen.getAndDecrement(),
////                    timestampMillis = System.currentTimeMillis(),
////                    type = Memory.Type.TEXT,
////                    text = "Walked from Pacific Center... French Toast!!!"
////                )
////                s.memories += Memory(
////                    idGen.getAndIncrement(),
////                    timestampMillis = System.currentTimeMillis(),
////                    type = Memory.Type.TEXT,
////                    text = "Later... doughnuts. Delicious!"
////                )
//                return s
//            }
//            stories += seed("Sad Day! Mish Mish Gone", null)
//            stories += seed("Passed Driving Test!", null)
//            stories += seed("Stanley Park Vancouver", "Vancouver")
//            stories += seed("Stanley Park Vancouver", "Vancouver")
//            stories += seed("Stanley Park Vancouver", "Vancouver")
//        }

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
                    val createMemory = newEffectToCreateMemory()
                    val createStory = newEffectToCreateStory()
                    val addMemoryToStory = newEffectToAddMemoryToStory()

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

                                val (loaded, memories) = getMemories()
                                val (loaded2, stories) = getStories()

                                if (!loaded || !loaded2) {
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
                                        addMemoryToStory(story, memory)
                                    }
                                )
                            }


                            composable("list") {
                                currentTitle = "My Stories"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null

                                val (loaded, stories) = getStories()

                                if (!loaded) {
                                    Surface { Text("Loading...") }
                                    return@composable
                                }

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
                                        createStory(newStory)
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

                                val (loaded, story) = getStoryById(id)
                                if (!loaded) {
                                    Surface { Text("Loading...") }
                                    return@composable
                                }

                                if (story == null) {
                                    Log.e("StoryDetailScreen", "Story with id $id not found")
                                    Surface { Text("Story not found...") }
                                    return@composable
                                }

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

                                val (loaded, story) = getStoryById(id)
                                if (!loaded) {
                                    Surface { Text("Loading...") }
                                    return@composable
                                }

                                if (story == null) {
                                    Log.e("StoryDetailScreen", "Story with id $id not found")
                                    Surface { Text("Story not found...") }
                                    return@composable
                                }

                                SegmentEditorScreen(
                                    onCancel = { nav.popBackStack() },
                                    onSubmit = { text, uri: Uri? ->
                                        // TODO: Re-enable this
//                                        story.memories.add(
//                                            0,
//                                            Memory(
//                                                idGen.getAndIncrement(),
//                                                timestampMillis = System.currentTimeMillis(),
//                                                type = Memory.Type.TEXT,
//                                                text = text,
//                                                imageUri = uri?.toString(),
//                                            )
//                                        )
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
