@file:OptIn(ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.demo

import android.Manifest
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.InterpreterMode
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import coolio.zoewong.traverse.database.AUTOMATICALLY_GENERATED_ID
import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.database.StoryEntity
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.ui.demo.AppShell
import coolio.zoewong.traverse.ui.demo.CreateStoryScreen
import coolio.zoewong.traverse.ui.demo.JournalScreen
import coolio.zoewong.traverse.ui.demo.MapScreen
import coolio.zoewong.traverse.ui.demo.SegmentEditorScreen
import coolio.zoewong.traverse.ui.demo.SettingsScreen
import coolio.zoewong.traverse.ui.demo.StoryDetailScreen
import coolio.zoewong.traverse.ui.demo.StoryListScreen
import coolio.zoewong.traverse.ui.provider.getMemories
import coolio.zoewong.traverse.ui.provider.getMemoriesManager
import coolio.zoewong.traverse.ui.provider.getStories
import coolio.zoewong.traverse.ui.provider.getStoriesManager
import coolio.zoewong.traverse.ui.state.AppState
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.getStoryAnalysisService
import coolio.zoewong.traverse.ui.state.SplashScreenStateProvider
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
import coolio.zoewong.traverse.ui.demo.StoryDetailScreenMenu

class TraverseDemoActivity : ComponentActivity() {

    val idGen = AtomicLong(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme(this)

        fun Double.format(decimals: Int): String =
            "%.${decimals}f".format(this)

        setContent {
            TraverseTheme {
                SplashScreenStateProvider {

                    AppState(activity = this@TraverseDemoActivity) {
                        val nav = rememberNavController()
                        var currentTitle by remember { mutableStateOf("Journal") }
                        var currentSubtitle by remember { mutableStateOf<String?>(null) }
                        var customNavigationIcon by remember {
                            mutableStateOf<(@Composable () -> Unit)?>(null)
                        }
                        var customActions by remember {
                            mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)
                        }

                        AppShell(
                            nav = nav,
                            currentTitle = currentTitle,
                            subtitle = currentSubtitle,
                            navigationIcon = customNavigationIcon,
                            actions = customActions
                        ) {
                            val context = LocalContext.current

                            NavHost(
                                navController = nav,
                                startDestination = "journal"
                            ) {
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
                                        onSend = { text, uri ->
                                            memoriesManager.fromCallback { db ->
                                                val savedUri = uri?.let {
                                                    db.media.saveImage(context, it)
                                                }
                                                val (type, contents) = when {
                                                    text != null -> MemoryType.TEXT to text
                                                    savedUri != null -> MemoryType.IMAGE to savedUri.toString()
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
                                            }
                                        },
                                        onAddToStory = { memory, story ->
                                            storiesManager.fromCallback {
                                                addMemoryToStory(story, memory)
                                            }
                                        },
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

                                    val stories = getStories()

                                    MapScreen(
                                        stories = stories,
                                        onOpenStory = { id ->
                                            nav.navigate("detail/$id")
                                        }
                                    )
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
                                        onCreate = { title, locationName, cover, latLng ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val repo = dbState.waitForReady()

                                                val savedCoverUri = cover?.let {
                                                    repo.media.saveImage(context, it)
                                                }

                                                val storyEntity = StoryEntity(
                                                    id = 0L,
                                                    title = title,
                                                    timestamp = System.currentTimeMillis(),
                                                    coverUri = savedCoverUri,
                                                    location = latLng,
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
                                    val summaryVisible = remember(id) { mutableStateOf(false) }

                                    val storiesManager = getStoriesManager()
                                    val story = getStories().find { it.id == id }
                                    if (story == null) {
                                        Log.e("StoryDetailScreen", "Story with id $id not found")
                                        Surface { Text("Story not found...") }
                                        return@composable
                                    }

                                    val context = LocalContext.current
                                    val dbState = DatabaseState.current
                                    val fusedLocationClient = remember {
                                        LocationServices.getFusedLocationProviderClient(context)
                                    }
                                    val scope = rememberCoroutineScope()

                                    fun persistStoryLocation(latLng: LatLng) {
                                        scope.launch(Dispatchers.IO) {
                                            val repo = dbState.waitForReady()
                                            val entity = repo.stories.get(story.id) ?: return@launch

                                            val updated = entity.copy(
                                                location = latLng,
                                                locationName = entity.locationName
                                                    ?: "(${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})"
                                            )
                                            repo.stories.update(updated)
                                        }
                                    }

                                    val locationPermissionLauncher =
                                        rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.RequestPermission()
                                        ) { granted ->
                                            if (granted) {
                                                fusedLocationClient.lastLocation
                                                    .addOnSuccessListener { loc ->
                                                        if (loc != null) {
                                                            persistStoryLocation(
                                                                LatLng(
                                                                    loc.latitude,
                                                                    loc.longitude
                                                                )
                                                            )
                                                            Toast.makeText(
                                                                context,
                                                                "Story location updated",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "No location available",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to get location",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Location permission denied",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                    fun updateStoryLocationWithCurrent() {
                                        val hasPermission =
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            fusedLocationClient.lastLocation
                                                .addOnSuccessListener { loc ->
                                                    if (loc != null) {
                                                        persistStoryLocation(
                                                            LatLng(
                                                                loc.latitude,
                                                                loc.longitude
                                                            )
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Story location updated",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "No location available",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to get location",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            locationPermissionLauncher.launch(
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        }
                                    }

                                    currentTitle = story.title
                                    currentSubtitle =
                                        SimpleDateFormat(
                                            "MMMM d'th', yyyy",
                                            Locale.getDefault()
                                        ).format(Date(story.timestamp))

                                    customNavigationIcon = {
                                        IconButton(onClick = { nav.popBackStack() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    }
                                    customActions = {
                                        IconButton(onClick = { updateStoryLocationWithCurrent() }) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = "Set story location"
                                            )
                                        }
                                        StoryDetailScreenMenu(
                                            story = story,
                                            navController = nav,
                                            summaryVisibleState = summaryVisible,
                                        )
                                    }

                                StoryDetailScreen(
                                    story = story,
                                    onBack = { nav.popBackStack() },
                                    onAddToStory = { nav.navigate("add/$id") },
                                    showSummary = summaryVisible.value,
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
                                                val savedUri = uri?.let {
                                                    db.media.saveImage(context, it)
                                                }
                                                val (type, contents) = when {
                                                    savedUri != null -> MemoryType.IMAGE to savedUri.toString()
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
}
