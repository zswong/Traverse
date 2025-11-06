@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coolio.zoewong.traverse.R
import coolio.zoewong.traverse.model.Segment
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.ui.demo.AppShell
import coolio.zoewong.traverse.ui.demo.ChatMsg
import coolio.zoewong.traverse.ui.demo.JournalScreen
import coolio.zoewong.traverse.ui.demo.CreateStoryScreen
import coolio.zoewong.traverse.ui.demo.SegmentEditorScreen
import coolio.zoewong.traverse.ui.demo.StoryDetailScreen
import coolio.zoewong.traverse.ui.demo.StoryListScreen
import java.util.concurrent.atomic.AtomicLong

class TraverseDemoActivity : ComponentActivity() {

    private val idGen = AtomicLong(1)
    private val stories = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (stories.isEmpty()) {
            fun seed(title: String, location: String?): Story {
                val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
                s.segments += Segment(idGen.getAndIncrement(), System.currentTimeMillis(), "Walked from Pacific Center... French Toast!!!")
                s.segments += Segment(idGen.getAndIncrement(), System.currentTimeMillis(), "Later... doughnuts. Delicious!")
                return s
            }
            stories += seed("Sad Day! Mish Mish Gone", null)
            stories += seed("Passed Driving Test!", null)
            stories += seed("Stanley Park Vancouver", "Vancouver")
            stories += seed("Stanley Park Vancouver", "Vancouver")
            stories += seed("Stanley Park Vancouver", "Vancouver")
        }

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                var currentTitle by remember { mutableStateOf("Journal") }

                AppShell(
                    nav = nav,
                    currentTitle = currentTitle
                ) {
                    NavHost(navController = nav, startDestination = "journal") {

                        composable("journal") {
                            currentTitle = "Journal"
                            var msgs by remember {
                                mutableStateOf(
                                    listOf(
                                        ChatMsg(1, "Walked from Pacific Center to this cafe and had the most delicious French Toast ever!!!!", null),
                                        ChatMsg(2, null, R.drawable.coffee),
                                        ChatMsg(3, "Later... doughnuts. Delicious!", null),
                                        ChatMsg(4, null, R.drawable.coffeewithdonuts),
                                    )
                                )
                            }
                            JournalScreen(
                                messages = msgs,
                                onSend = { text, resId ->
                                    val nextId = (msgs.maxOfOrNull { it.id } ?: 0L) + 1
                                    msgs = msgs + ChatMsg(nextId, text, resId)
                                }
                            )
                        }

                        composable("list") {
                            currentTitle = "My Stories"
                            StoryListScreen(
                                stories = stories,
                                onOpen = { id -> nav.navigate("detail/$id") },
                                onCreate = { nav.navigate("create") }
                            )
                        }

                        composable("create") {
                            currentTitle = "Create Story"
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
                            Surface {
                                Text("Settings (coming soon)", modifier = Modifier.padding(24.dp))
                            }
                        }

                        composable(
                            route = "detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStack ->
                            val id = backStack.arguments!!.getLong("id")
                            val story = stories.first { it.id == id }
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
