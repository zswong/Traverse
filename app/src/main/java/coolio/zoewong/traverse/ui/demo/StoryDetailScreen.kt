package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coolio.zoewong.traverse.database.StorySegmentEntity
import coolio.zoewong.traverse.model.Story
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.LoadStatus
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    story: Story,
    onBack: () -> Unit,
    onAddToStory: () -> Unit
) {
    val dbState = DatabaseState.current

    // 从 Room 里读出的 segment 列表
    var segments by remember { mutableStateOf<List<StorySegmentEntity>>(emptyList()) }

    // 监听数据库加载 & storyId 变化，然后订阅 Flow
    LaunchedEffect(dbState.status, story.id) {
        if (dbState.status == LoadStatus.LOADED) {
            val repo = dbState.database
            repo.watchStorySegments(story.id).collectLatest { list ->
                segments = list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2
                        )
                        val dateText = SimpleDateFormat(
                            "EEEE, MMM dd, yyyy",
                            Locale.getDefault()
                        ).format(Date(story.dateMillis))
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddToStory) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add segment"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (!story.location.isNullOrBlank()) {
                Text(
                    text = story.location!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            when {
                dbState.status != LoadStatus.LOADED -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                segments.isEmpty() -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No moments have been added to this story yet.\n\n" +
                                    "Long-press a journal entry or tap + to attach one.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(segments, key = { it.id }) { seg ->
                            StorySegmentCard(seg = seg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorySegmentCard(seg: StorySegmentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            val timeText = SimpleDateFormat(
                "MMM dd, yyyy • HH:mm",
                Locale.getDefault()
            ).format(Date(seg.createdAt))

            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )


            if (!seg.text.isNullOrBlank()) {
                Text(
                    text = seg.text!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            }


            if (!seg.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = seg.imageUri,
                    contentDescription = "Story image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .padding(top = 4.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
