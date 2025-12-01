package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import coolio.zoewong.traverse.model.Story
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

@Composable
fun MapScreen(
    stories: List<Story>,
    onOpenStory: (Long) -> Unit
) {
    val storiesWithLocation = stories.filter { it.location != null }

    Log.d("MapScreen", "storiesWithLocation size = ${storiesWithLocation.size}")

    val initialLatLng: LatLng = storiesWithLocation.firstOrNull()?.location
        ?: LatLng(49.2827, -123.1207)

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 11f)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (storiesWithLocation.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No stories have locations yet.\nAdd a location to a story to see it here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // 先按时间排一下，方便路线从旧 -> 新
                val sorted = storiesWithLocation.sortedBy { it.timestamp }

                // 用来画线：记录“偏移后”的 marker 坐标
                val linePoints = mutableListOf<LatLng>()

                // 画 markers，并同时把偏移后的坐标塞进 linePoints
                sorted.forEachIndexed { index, story ->
                    val base = story.location!!
                    // 轻微偏移，避免完全重叠
                    val offsetLat = base.latitude + index * 0.0001
                    val offsetLng = base.longitude + index * 0.0001
                    val markerPos = LatLng(offsetLat, offsetLng)

                    linePoints += markerPos   // ⭐ 线用的就是这个点

                    val title = story.title
                    val dateText = SimpleDateFormat(
                        "MMM dd, yyyy",
                        Locale.getDefault()
                    ).format(Date(story.timestamp))

                    Marker(
                        state = MarkerState(position = markerPos),
                        title = title,
                        snippet = story.locationName ?: dateText,
                        onClick = {
                            onOpenStory(story.id)
                            false
                        }
                    )
                }

                // 再把这些“可见的点”连成一条折线
                if (linePoints.size >= 2) {
                    Polyline(
                        points = linePoints,
                        width = 6f
                        // 想的话可以加 color = Color.Red
                    )
                }
            }
        }
    }
}
