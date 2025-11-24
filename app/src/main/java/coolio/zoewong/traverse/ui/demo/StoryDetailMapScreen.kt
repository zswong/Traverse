package coolio.zoewong.traverse.ui.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.drawable.toBitmap
import android.Manifest
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import coolio.zoewong.traverse.database.StoryEntity
import coolio.zoewong.traverse.model.viewmodel.getStoryEntities
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.util.ImageLocationExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StoryDetailMapScreen(
    storyId: Long,
    onStoryClick: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val (loaded, storyEntities) = getStoryEntities()
    
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(locationPermissionState.status) {
        when {
            locationPermissionState.status.isGranted && !locationLoaded -> {
                try {
                    val locationClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = suspendCancellableCoroutine<Location?> { continuation ->
                        locationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        ).addOnSuccessListener { location ->
                            continuation.resume(location)
                        }.addOnFailureListener { exception ->
                            continuation.resumeWithException(exception)
                        }
                    }
                    
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                    }
                } catch (e: Exception) {
                } finally {
                    locationLoaded = true
                }
            }
            !locationPermissionState.status.isGranted -> {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
    
    val storyEntity = remember(storyEntities, storyId) {
        storyEntities.firstOrNull { it.id == storyId }
    }
    
    data class StoryMarker(
        val story: StoryEntity,
        val location: LatLng,
        val icon: BitmapDescriptor
    )
    
    var storyMarkers by remember { mutableStateOf<List<StoryMarker>>(emptyList()) }
    var isLoadingMarkers by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dbstate = DatabaseState.current
    
    LaunchedEffect(storyEntities) {
        if (storyEntities.isEmpty()) {
            storyMarkers = emptyList()
            isLoadingMarkers = false
            return@LaunchedEffect
        }
        
        isLoadingMarkers = true
        val markers = mutableListOf<StoryMarker>()
        val imageLoader = ImageLoader(context)
        
        withContext(Dispatchers.IO) {
            storyEntities.forEach { story ->
                var location: LatLng? = story.location
                
                if (location == null && story.coverUri != null) {
                    try {
                        location = ImageLocationExtractor.extractLocation(context, story.coverUri)
                    } catch (e: Exception) {
                        location = null
                    }
                }
                
                if (location != null) {
                    val icon = if (story.coverUri != null) {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(story.coverUri)
                                .size(120, 120)
                                .build()
                            
                            val result = imageLoader.execute(request)
                            if (result is SuccessResult) {
                                val bitmap = result.drawable.toBitmap()
                                createCircularMarkerIcon(bitmap)
                            } else {
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            }
                        } catch (e: Exception) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        }
                    } else {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    }
                    
                    markers.add(StoryMarker(story, location, icon))
                }
            }
        }
        
        Log.d("StoryDetailMapScreen", "Created ${markers.size} markers from ${storyEntities.size} stories")
        
        storyMarkers = markers
        isLoadingMarkers = false
    }
    
    val highlightedStoryMarker = remember(storyMarkers, storyId) {
        storyId.let { id ->
            storyMarkers.firstOrNull { it.story.id == id }
        }
    }
    
    val cameraPositionState = rememberCameraPositionState()
    var cameraInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(storyMarkers.size, storyId, highlightedStoryMarker, loaded, isLoadingMarkers) {
        if (!loaded || isLoadingMarkers) return@LaunchedEffect
        
        if (highlightedStoryMarker != null) {
            if (!cameraInitialized) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(highlightedStoryMarker.location, 14f)
                )
                cameraInitialized = true
            }
        } else if (!cameraInitialized) {
            if (storyMarkers.isNotEmpty()) {
                val bounds = LatLngBounds.builder()
                storyMarkers.forEach { marker ->
                    bounds.include(marker.location)
                }
                
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                    )
                } catch (e: Exception) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        storyMarkers.first().location,
                        10f
                    )
                }
                cameraInitialized = true
            } else {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(37.7749, -122.4194),
                    10f
                )
                cameraInitialized = true
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!loaded || isLoadingMarkers) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val uiSettings = remember {
                MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = true
                )
            }
            
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings,
                properties = remember {
                    MapProperties(
                        isMyLocationEnabled = false
                    )
                }
            ) {
                storyMarkers.forEach { marker ->
                    val markerState = remember(marker.story.id) { MarkerState(position = marker.location) }
                    var lastSavedPosition by remember(marker.story.id) { mutableStateOf<LatLng?>(marker.location) }
                    var saveJob by remember(marker.story.id) { mutableStateOf<Job?>(null) }
                    
                    LaunchedEffect(marker.location) {
                        if (markerState.position != marker.location) {
                            markerState.position = marker.location
                            lastSavedPosition = marker.location
                        }
                    }
                    
                    LaunchedEffect(markerState.position) {
                        val currentPosition = markerState.position
                        if (currentPosition != lastSavedPosition) {
                            saveJob?.cancel()
                            saveJob = scope.launch {
                                delay(500)
                                try {
                                    val db = dbstate.waitForReady()
                                    val locationName = ImageLocationExtractor.getLocationName(
                                        context,
                                        currentPosition
                                    )
                                    val updatedStory = marker.story.copy(
                                        location = currentPosition,
                                        locationName = locationName
                                    )
                                    db.stories.update(updatedStory)
                                    lastSavedPosition = currentPosition
                                    Log.d("StoryDetailMapScreen", "Updated story '${marker.story.title}' location to ${currentPosition.latitude}, ${currentPosition.longitude}")
                                } catch (e: Exception) {
                                    Log.e("StoryDetailMapScreen", "Failed to save marker location", e)
                                }
                            }
                        }
                    }
                    
                    Marker(
                        state = markerState,
                        title = marker.story.title,
                        snippet = marker.story.locationName ?: "${markerState.position.latitude}, ${markerState.position.longitude}",
                        icon = marker.icon,
                        draggable = true,
                        onClick = {
                            onStoryClick?.invoke(marker.story.id)
                            true
                        },
                        onInfoWindowClick = {
                            onStoryClick?.invoke(marker.story.id)
                        }
                    )
                }
            }
        }
    }
}

private fun createCircularMarkerIcon(bitmap: Bitmap): BitmapDescriptor {
    val size = 120
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    val rect = Rect(0, 0, size, size)
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
    
    val centerX = size / 2f
    val centerY = size / 2f
    val radius = size / 2f
    
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, radius, paint)
    
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaledBitmap, rect, rect, paint)
    
    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, radius - 2f, paint)
    
    return BitmapDescriptorFactory.fromBitmap(output)
}

