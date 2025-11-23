@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.ui.demo

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.annotation.DrawableRes
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import coolio.zoewong.traverse.ui.state.DatabaseState

data class ChatMsg(val id: Long, val text: String?, @DrawableRes val imageRes: Int?)

@Composable
fun JournalScreen(
    messages: List<ChatMsg>,
    onSend: (String?, Uri?) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showAttach by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!m.text.isNullOrBlank()) Text(m.text!!)
                        if (m.imageRes != null) {
                            AsyncImage(
                                model = m.imageRes,
                                contentDescription = "photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }


        Surface(shadowElevation = 6.dp) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAttach = true }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "attach")
                }
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Your message...") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (input.isNotBlank()) {
                        onSend(input, null)
                        input = ""
                    }
                }) { Text("Send") }
            }
        }
    }

    if (showAttach) {
        AttachmentSheet(
            onDismiss = { showAttach = false },
            onPick = { uri ->
                onSend(null, uri)
                showAttach = false
            }
        )
    }
}

@Composable
private fun AttachmentSheet(
    onDismiss: () -> Unit,
    onPick: (Uri) -> Unit
) {

    val candidates = listOf(
        coolio.zoewong.traverse.R.drawable.coffee,
        coolio.zoewong.traverse.R.drawable.coffeewithdonuts
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pick a photo", style = MaterialTheme.typography.titleMedium)

            AttachmentSheetItemTakePhoto(onPick = onPick)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AttachmentSheetItemTakePhoto(
    onPick: (Uri) -> Unit
) {
    val context = LocalContext.current
    var cameraPhotoURI by rememberSaveable {
        mutableStateOf<Uri?>(null)
    }

    val takePictureResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { ok ->
            if (ok) {
                cameraPhotoURI?.let {
                    onPick(it)
                }
            }
        },
    )

    fun takePicture() {
        val uri = cameraPhotoURI
        if (uri == null) {
            Toast.makeText(context, "Database is not loaded yet.", Toast.LENGTH_LONG).show()
            return
        }

        takePictureResult.launch(uri)
    }

    val cameraPermission = rememberPermissionState(
        android.Manifest.permission.CAMERA,
        onPermissionResult = { granted ->
            if (granted) {
                takePicture()
            }
        }
    )

    fun askForPermissionThenTakePicture() {
        cameraPermission.launchPermissionRequest() // calls takePicture if granted
    }

    DatabaseState.current.whenReady { db ->
        if (cameraPhotoURI == null) {
            cameraPhotoURI = db.media.uriForIntentResult(context, "camera-photo")
            Log.d("AttachmentSheetItemTakePhoto", "Photo should be saved to $cameraPhotoURI")
        }
    }

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (cameraPermission.status.isGranted) {
                    true -> takePicture()
                    false -> askForPermissionThenTakePicture()
                }
            }
    ) {
        Text("Camera")
    }
}

