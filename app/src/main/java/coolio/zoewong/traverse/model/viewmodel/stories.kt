package coolio.zoewong.traverse.model.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.database.StoryEntity
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.model.Memory
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.model.toDatabase
import coolio.zoewong.traverse.model.toModel
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.util.ImageLocationExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

suspend fun toCreateStory(db: TraverseRepository, story: Story, context: Context): Long {
    val storyEntity = story.toDatabase()
    
    val hasLocationPermission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    
    val location = if (hasLocationPermission) {
        try {
            val locationClient = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine<LatLng?> { continuation ->
                locationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    location?.let {
                        continuation.resume(LatLng(it.latitude, it.longitude))
                    } ?: continuation.resume(null)
                }.addOnFailureListener { exception ->
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
    
    val updatedStoryEntity = storyEntity.copy(
        location = location,
        locationName = story.location ?: location?.let {
            ImageLocationExtractor.getLocationName(context, it)
        }
    )
    
    val createdStory = db.stories.insert(updatedStoryEntity)
    return createdStory.id
}

suspend fun toAddMemoryToStory(db: TraverseRepository, story: Story, memory: Memory, context: Context) {
    db.stories.addMemory(
        story.toDatabase(),
        memory.toDatabase(),
    )
    
    val storyEntity = db.stories.get(story.id) ?: return
    
    // If the memory being added is an image, update the story location with its geolocation
    if (memory.type == Memory.Type.IMAGE && memory.imageUri != null) {
        val imageUri = Uri.parse(memory.imageUri)
        
        val location = ImageLocationExtractor.extractLocation(context, imageUri)
        val locationName = location?.let { 
            ImageLocationExtractor.getLocationName(context, it) 
        }
        
        // Log the location information
        if (location != null) {
            Log.d("StoryLocation", "Photo added to story '${story.title}' (ID: ${story.id})")
            Log.d("StoryLocation", "  Latitude: ${location.latitude}")
            Log.d("StoryLocation", "  Longitude: ${location.longitude}")
            Log.d("StoryLocation", "  Address: ${locationName ?: "Unknown"}")
        } else {
            Log.d("StoryLocation", "Photo added to story '${story.title}' (ID: ${story.id}) - No location data found in photo")
        }
        
        // Update story with the photo's location (always update, even if location is null)
        val updatedStory = storyEntity.copy(
            location = location,
            locationName = locationName
        )
        db.stories.update(updatedStory)
    }
    
    // Also update cover image if this is the first image memory
    val allMemories = db.stories.watchMemoriesOf(storyEntity).first()
    val firstImageMemory = allMemories
        .filter { it.type == MemoryType.IMAGE }
        .minByOrNull { it.timestamp }
    
    if (firstImageMemory != null && storyEntity.coverUri == null) {
        val imageUri = Uri.parse(firstImageMemory.contents)
        
        val updatedStory = storyEntity.copy(
            coverUri = imageUri
        )
        db.stories.update(updatedStory)
    }
}

/**
 * Removes the given memory from the given story.
 */
suspend fun toRemoveMemoryFromStory(db: TraverseRepository, story: Story, memory: Memory) {
    db.stories.removeMemory(
        story.toDatabase(),
        memory.toDatabase(),
    )
}

/**
 * Deletes the given story from the database.
 */
suspend fun toDeleteStory(db: TraverseRepository, story: Story) {
    db.stories.delete(story.toDatabase())
}

/**
 * Gets all StoryEntity instances from the database.
 * Useful when you need direct access to LatLng coordinates.
 */
@Composable
fun getStoryEntities(): Pair<Boolean, List<StoryEntity>> {
    val dbstate = DatabaseState.current

    var loaded by remember { mutableStateOf(false) }
    var storyEntities by remember {
        mutableStateOf(listOf<StoryEntity>())
    }

    dbstate.whenReady { db ->
        LaunchedEffect(db) {
            db.stories.watchAll().collect { entities ->
                storyEntities = entities
                loaded = true
            }
        }
    }

    return loaded to storyEntities
}
