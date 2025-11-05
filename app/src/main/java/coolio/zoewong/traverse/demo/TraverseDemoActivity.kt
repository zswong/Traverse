@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import coolio.zoewong.traverse.model.Segment
import coolio.zoewong.traverse.model.Story
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
        }

        setContent {
            MaterialTheme {
                Surface {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "list") {
                        composable("list") {
                            StoryListScreen(
                                stories = stories,
                                onOpen = { nav.navigate("detail/$it") },
                                onCreate = { nav.navigate("create") }
                            )
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
                                onSubmit = { text ->
                                    story.segments.add(0, Segment(idGen.getAndIncrement(), System.currentTimeMillis(), text))
                                    nav.popBackStack()
                                }
                            )
                        }
                        composable("create") {
                            CreateStoryScreen(
                                onCancel = { nav.popBackStack() },
                                onCreate = { title, location ->
                                    val s = Story(idGen.getAndIncrement(), title, System.currentTimeMillis(), location)
                                    stories.add(0, s)
                                    nav.navigate("detail/${s.id}") {
                                        popUpTo("list") { inclusive = false }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
