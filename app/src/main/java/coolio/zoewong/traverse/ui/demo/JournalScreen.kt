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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import coolio.zoewong.traverse.ui.state.DatabaseState
import androidx.compose.foundation.shape.RoundedCornerShape
import coolio.zoewong.traverse.model.Story
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

data class ChatMsg(val id: Long, val text: String?, val imageUri: String? = null, val timestamp: Long)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(
    messages: List<ChatMsg>,
    stories: List<Story>,
    onSend: (String?, Uri?) -> Unit,
    onAddToStory: (ChatMsg, Story) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showAttach by remember { mutableStateOf(false) }


    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showStoryPicker by remember { mutableStateOf(false) }

    //Speech to text
    var isListening by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectionMode = selectedIds.isNotEmpty()

    //Speech Recognizer
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context)}

    //to start speech recognition
    fun startSpeechRecognition(recognizer: SpeechRecognizer, context: Context) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // force offline
        }
        /* //breach privacy?? only work for all devices that have offline speech recognition installed
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(intent, 0)
        if (activities.isEmpty()) {
            Toast.makeText(
                context,
                "Offline speech recognition is not available on this device",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        */

        try {
            recognizer.startListening(intent)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }


    // permission launcher for microphone
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(speechRecognizer, context)
            isListening = true
        } else {
            // Permission denied
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    //listener
    val speechListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        Toast.makeText(context, "No speech recognized", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val spokenText = matches[0]
                        input = spokenText
                        Toast.makeText(context, "Voice input received", Toast.LENGTH_SHORT).show()
                    }
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // Set up speech recognizer
    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(speechListener)
    }
    //Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    //to handle speech recognition toggle
    fun toggleSpeechRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startSpeechRecognition(speechRecognizer, context)
            } else {
                microphonePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }



    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                val interactionSource = remember { MutableInteractionSource() }
                val isSelected = m.id in selectedIds

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (selectionMode) {

                                    selectedIds =
                                        if (isSelected) selectedIds - m.id
                                        else selectedIds + m.id
                                } else {

                                }
                            },
                            onLongClick = {

                                selectedIds =
                                    if (isSelected) selectedIds - m.id
                                    else selectedIds + m.id
                            }
                        )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeText = remember(m.timestamp) {
                            SimpleDateFormat(
                                "MMM dd, yyyy • HH:mm",
                                Locale.getDefault()
                            ).format(Date(m.timestamp))
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (!m.text.isNullOrBlank()) {
                            Text(m.text)
                        }
                        if (!m.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = m.imageUri,
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


        if (selectionMode) {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        enabled = stories.isNotEmpty(),
                        onClick = { showStoryPicker = true }
                    ) {
                        Text("Add to story")
                    }
                }
            }
        }
        Surface(shadowElevation = 6.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAttach = true }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "attach")
                }
                IconButton(
                    onClick = { toggleSpeechRecognition() }
                ) {
                    Icon(
                        if (isListening) Icons.Outlined.Mic else Icons.Outlined.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Start voice input",
                        tint = if (isListening) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Your message...") },
                    shape = RoundedCornerShape(23.dp)
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


    if (showStoryPicker && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showStoryPicker = false
            },
            title = { Text("Add to a story") },
            text = {
                if (stories.isEmpty()) {
                    Text("You don't have any stories yet.\nCreate a story first.")
                } else {
                    Column {
                        Text(
                            "Choose a story. The selected journal entries will be attached to it.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(stories, key = { it.id }) { story ->
                                TextButton(
                                    onClick = {

                                        val selectedMessages = messages.filter { it.id in selectedIds }
                                        selectedMessages.forEach { msg ->
                                            onAddToStory(msg, story)
                                        }


                                        showStoryPicker = false
                                        selectedIds = emptySet()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            text = story.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        val dateText = SimpleDateFormat(
                                            "MMM dd, yyyy",
                                            Locale.getDefault()
                                        ).format(Date(story.dateMillis))
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStoryPicker = false

                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun AttachmentSheet(
    onDismiss: () -> Unit,
    onPick: (Uri) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Pick a photo", style = MaterialTheme.typography.titleMedium)

            AttachmentSheetItemTakePhoto(onPick = onPick)


            AttachmentSheetItemPickFromGallery(onPick = onPick)

            Spacer(Modifier.height(16.dp))
        }
    }
}
@Composable
private fun AttachmentSheetItemPickFromGallery(
    onPick: (Uri) -> Unit
) {
    val context = LocalContext.current

    val pickImageResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                onPick(uri)
            }
        }
    )

    fun pickFromGallery() {
        pickImageResult.launch("image/*")
    }

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                pickFromGallery()
            }
    ) {
        Text("Gallery")
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

