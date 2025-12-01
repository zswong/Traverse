package coolio.zoewong.traverse.ui.demo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onCancel: () -> Unit,
    onCreate: (String, String?, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }


    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            coverUri = uri
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Story") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 标题
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Story title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 位置名称（可选）
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 封面图区域
            Text(
                text = "Cover image (optional)",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (coverUri != null) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = "Story cover preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No cover selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 选择/更换封面按钮
            OutlinedButton(
                onClick = { pickCoverLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (coverUri == null) "Choose from gallery" else "Change cover")
            }

            Spacer(Modifier.height(8.dp))

            // 底部操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(
                                name,
                                location.ifBlank { null },
                                coverUri      // ⭐ 把封面传出去
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank()
                ) {
                    Text("Create Story")
                }
            }
        }
    }
}
