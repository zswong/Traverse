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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Update
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
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.ui.demo.AppShell
import coolio.zoewong.traverse.ui.demo.JournalScreen
import coolio.zoewong.traverse.ui.demo.SegmentEditorScreen
import coolio.zoewong.traverse.ui.demo.StoryDetailScreen
import coolio.zoewong.traverse.ui.demo.StoryListScreen
import coolio.zoewong.traverse.ui.demo.CreateStoryScreen
import coolio.zoewong.traverse.ui.demo.SettingsScreen
import coolio.zoewong.traverse.ui.demo.MapScreen
import coolio.zoewong.traverse.ui.provider.getMemories
import coolio.zoewong.traverse.ui.provider.getMemoriesManager
import coolio.zoewong.traverse.ui.provider.getStories
import coolio.zoewong.traverse.ui.provider.getStoriesManager
import coolio.zoewong.traverse.ui.state.AppState
import coolio.zoewong.traverse.ui.state.getStoryAnalysisService
import coolio.zoewong.traverse.ui.theme.ThemeManager
import coolio.zoewong.traverse.ui.theme.TraverseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import coolio.zoewong.traverse.database.StoryEntity
import coolio.zoewong.traverse.ui.state.DatabaseState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            TraverseTheme {
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

                AppState(activity = this) {
                    AppShell(
                        nav = nav,
                        currentTitle = currentTitle,
                        subtitle = currentSubtitle,
                        navigationIcon = customNavigationIcon,
                        actions = customActions
                    ) {
                        val context = LocalContext.current
                        NavHost(navController = nav, startDestination = "journal") {

                            composable("journal") {
                                currentTitle = "Journal"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null

                                val memoriesManager = getMemoriesManager()
                                val storiesManager = getStoriesManager()
                                val storyAnalysisService = getStoryAnalysisService()

                                JournalScreen(
                                    memories = getMemories(),
                                    stories = getStories(),
                                    onSend = { text, uri -> memoriesManager.fromCallback { db ->
                                        val uri = uri?.let {
                                            db.media.saveImage(context, it)
                                        }
                                        val (type, contents) = when {
                                            text != null -> MemoryType.TEXT to text
                                            uri != null -> MemoryType.IMAGE to uri.toString()
                                            else -> throw IllegalArgumentException("No message or image?")
                                        }
                                        createMemory(
                                            Memory(
                                                id = AUTOMATICALLY_GENERATED_ID,
                                                timestamp = Calendar.getInstance().time.time,
                                                contents = contents,
                                                type = type,
                                            )
                                        )
                                    }},
                                    onAddToStory = { memory, story -> storiesManager.fromCallback {
                                        addMemoryToStory(story, memory)
                                    }},
                                )
                            }


                            composable("list") {
                                currentTitle = "My Stories"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null

                                StoryListScreen(
                                    stories = getStories(),
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

                                val context = LocalContext.current
                                val dbState = DatabaseState.current

                                CreateStoryScreen(
                                    onCancel = { nav.popBackStack() },
                                    onCreate = { title, locationName, cover ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val repo = dbState.waitForReady()

                                            val savedCoverUri = cover?.let {
                                                repo.media.saveImage(context, it)
                                            }

                                            val storyEntity = StoryEntity(
                                                title = title,
                                                timestamp = System.currentTimeMillis(),
                                                coverUri = savedCoverUri,
                                                location = null,
                                                locationName = locationName
                                            )

                                            val inserted = repo.stories.insert(storyEntity)

                                            withContext(Dispatchers.Main) {
                                                nav.navigate("detail/${inserted.id}") {
                                                    popUpTo("list") { inclusive = false }
                                                }
                                            }
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

                                val storiesManager = getStoriesManager()
                                val story = getStories().find { it.id == id }
                                if (story == null) {
                                    Log.e("StoryDetailScreen", "Story with id $id not found")
                                    Surface { Text("Story not found...") }
                                    return@composable
                                }

                                currentTitle = story.title
                                currentSubtitle =
                                    SimpleDateFormat("MMMM d'th', yyyy", Locale.getDefault())
                                        .format(Date(story.timestamp))
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
                                    IconButton(onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            storiesManager.reanalyzeStory(story)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Update,
                                            contentDescription = "Re-analyze Story"
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

                                val storiesManager = getStoriesManager()
                                val memoriesManager = getMemoriesManager()

                                val story = getStories().find { it.id == id }
                                if (story == null) {
                                    Log.e("StoryDetailScreen", "Story with id $id not found")
                                    Surface { Text("Story not found...") }
                                    return@composable
                                }

                                SegmentEditorScreen(
                                    onCancel = { nav.popBackStack() },
                                    onSubmit = { text, uri: Uri? ->
                                        memoriesManager.fromCallback { db ->
                                            val uri = uri?.let {
                                                db.media.saveImage(context, it)
                                            }
                                            val (type, contents) = when {
                                                uri != null -> MemoryType.IMAGE to uri.toString()
                                                text != null -> MemoryType.TEXT to text
                                                else -> throw IllegalArgumentException("No message or image?")
                                            }
                                            val memory = Memory(
                                                id = AUTOMATICALLY_GENERATED_ID,
                                                timestamp = System.currentTimeMillis(),
                                                type = type,
                                                contents = contents,
                                            )

                                            val createdMemory = createMemory(memory)
                                            storiesManager.addMemoryToStory(story, createdMemory)
                                        }

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
