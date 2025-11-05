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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(
    stories: List<Story>,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My Story") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate) { Text("Create a New Story") }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stories, key = { it.id }) { s ->
                Card(onClick = { onOpen(s.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (!s.location.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(s.location!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
