package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coolio.zoewong.traverse.model.Story
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    story: Story,
    onBack: () -> Unit,
    onAddToStory: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(story.title) },
                navigationIcon = { /* Add icon if needed */ }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onAddToStory) { Text("Add to Story") }
                    //  Camera/Gallery entry button（Phase-1 not need）
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    if (!story.location.isNullOrBlank())
                        Text(
                            story.location!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    val date = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(
                        Date(story.dateMillis)
                    )
                    Text(date, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(story.segments, key = { it.id }) { seg ->
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "9:30",
                            style = MaterialTheme.typography.labelLarge
                        ) // 视觉锚点，PDF 有“9:30”
                        Text(
                            "Journal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(seg.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
