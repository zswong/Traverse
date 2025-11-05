package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEditorScreen(
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit
) = SegmentEditorScreen(
    onCancel = onCancel,
    onSubmit = { text, _ -> onSubmit(text) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEditorScreen(
    onCancel: () -> Unit,
    onSubmit: (String, Uri? ) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add to Story") }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                placeholder = { Text("Your message...") },
                minLines = 6
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { /* TODO Camera (Phase-2) */ }) { Text("Camera") }
                OutlinedButton(onClick = { /* TODO Gallery (Phase-2) */ }) { Text("Gallery") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { if (text.isNotBlank()) onSubmit(text,null) }) { Text("Add to Story") }
            }
        }
    }
}
