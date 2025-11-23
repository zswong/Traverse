package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.ui.effect.fadingEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(
    stories: List<Story>,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit
) {

    var searchQuery by remember { mutableStateOf("") }


    val filteredStories = remember(searchQuery, stories) {
        if (searchQuery.isBlank()) {
            stories
        } else {
            stories.filter { story ->
                story.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate) { Text("Create a New Story") }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Search stories") },
                    singleLine = true
                )

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.05f to Color.Red,
                                0.95f to Color.Red,
                                1f to Color.Transparent
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredStories, key = { it.id }) { story ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            onClick = { onOpen(story.id) }
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        story.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val formattedDate =
                                        SimpleDateFormat(
                                            "MMMM dd, yyyy",
                                            Locale.getDefault()
                                        ).format(
                                            Date(story.dateMillis)
                                        )
                                    Text(
                                        formattedDate,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                bottomStart = 16.dp,
                                                bottomEnd = 16.dp
                                            )
                                        )
                                        .background(Color(0xFFE0E0E0))
                                ) {
                                    if (!story.coverUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = story.coverUri,
                                            contentDescription = "Story cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
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
}
