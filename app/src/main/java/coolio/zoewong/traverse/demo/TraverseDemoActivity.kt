@file:OptIn(ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coolio.zoewong.traverse.database.MemoryEntity
import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.model.Segment
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.ui.demo.AppShell
import coolio.zoewong.traverse.ui.demo.ChatMsg
import coolio.zoewong.traverse.ui.demo.JournalScreen
import coolio.zoewong.traverse.ui.demo.CreateStoryScreen
import coolio.zoewong.traverse.ui.demo.SegmentEditorScreen
import coolio.zoewong.traverse.ui.demo.StoryDetailScreen
import coolio.zoewong.traverse.ui.demo.StoryListScreen
import coolio.zoewong.traverse.ui.demo.SettingsScreen
import coolio.zoewong.traverse.ui.state.AppState
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.LoadStatus
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

    private val idGen = AtomicLong(1)
    private val stories = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme(this)

        if (stories.isEmpty()) {
            fun seed(title: String, location: String?): Story {
                val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
                s.segments += Segment(
                    idGen.getAndIncrement(),
                    System.currentTimeMillis(),
                    "Walked from Pacific Center... French Toast!!!"
                )
                s.segments += Segment(
                    idGen.getAndIncrement(),
                    System.currentTimeMillis(),
                    "Later... doughnuts. Delicious!"
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
                                val context = LocalContext.current
                                var msgs by remember { mutableStateOf(listOf<ChatMsg>()) }
                                val dbstate = DatabaseState.current
                                dbstate.whenReady { db ->
                                    LaunchedEffect(db) {
                                        db.watchMemories().collect { entities ->
                                            msgs = entities.map {
                                                ChatMsg(it.id, it.contents, null)
                                                // TODO: Use a URL instead of resource
                                            }
                                        }
                                    }
                                }

                                if (dbstate.status != LoadStatus.LOADED) {
                                    // Simple example of showing a loading placeholder.
                                    Surface {
                                        Text("Loading...")
                                    }
                                    return@composable
                                }

                                JournalScreen(
                                    messages = msgs,
                                    onSend = { text, uri ->
                                        // TODO: Image URL instead of resource

                                        // CoroutineScope is only safe when not used directly inside
                                        // a @Composable function, as it will repeatedly call it
                                        // every time the composable is recomposed.
                                        //
                                        // It is safe here because onSend is only run once in
                                        // response to use interaction.
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val memory = when {
                                                text != null -> MemoryEntity(
                                                        type = MemoryType.TEXT,
                                                        timestamp = Calendar.getInstance().time.time,
                                                        contents = text,
                                                    )
                                                uri != null -> {
                                                    val savedUri = dbstate.database.media.saveImage(context, uri)
                                                    MemoryEntity(
                                                        type = MemoryType.IMAGE,
                                                        timestamp = Calendar.getInstance().time.time,
                                                        contents = savedUri.toString(),
                                                    )
                                                }
                                                else -> throw IllegalArgumentException("No message or image?")
                                            }

                                            dbstate.waitForReady().apply {
                                                insertMemory(memory)
                                            }
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

                            composable("create") {
                                currentTitle = "Create Story"
                                currentSubtitle = null
                                customNavigationIcon = null
                                customActions = null
                                CreateStoryScreen(
                                    onCancel = { nav.popBackStack() },
                                    onCreate = { title, location ->
                                        val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
                                        stories.add(0, s)
                                        nav.navigate("detail/${s.id}") { popUpTo("list") { inclusive = false } }
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
                                        story.segments.add(
                                            0,
                                            Segment(idGen.getAndIncrement(), System.currentTimeMillis(), text, imageUri = uri?.toString())
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
