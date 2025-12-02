package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import coolio.zoewong.traverse.ui.theme.ThemeManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Backup
import coolio.zoewong.traverse.ui.state.DatabaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.Backup
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.ui.state.DatabaseState
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InterpreterMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.ui.text.style.TextAlign
import coolio.zoewong.traverse.ui.state.getSettings
import coolio.zoewong.traverse.ui.state.getSettingsManager
import coolio.zoewong.traverse.ui.state.isStoryAnalysisSupported

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val settings = getSettings()
    val settingsManager = getSettingsManager()

    val context = LocalContext.current
    val isDarkMode = ThemeManager.isDarkMode

    var permissionCheckCacheKey by remember { mutableStateOf(0) }
    val hasCameraPermission = remember(permissionCheckCacheKey) {
        derivedStateOf {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val hasMicrophonePermission = remember(permissionCheckCacheKey) {
        derivedStateOf {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val hasLocationPermission = remember(permissionCheckCacheKey) {
        derivedStateOf {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionCheckCacheKey++
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionCheckCacheKey++
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionCheckCacheKey++
    }

    val dbState = DatabaseState.current
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showPrivacyText by remember { mutableStateOf(false) }
    var showTermsText by remember { mutableStateOf(false) }
    var showSummarizeStoriesDisclaimer by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val repo = dbState.waitForReady()
                    repo.exportBackup(
                        contentResolver = context.contentResolver,
                        outputUri = uri
                    )
                }
            }
        }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val repo = dbState.waitForReady()
                    repo.importBackup(
                        contentResolver = context.contentResolver,
                        inputUri = uri
                    )
                }
                showImportConfirm = true
            }
        }
    )
    Scaffold(
        topBar = {

        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // App Settings Section
            Text(
                "App Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dark Mode
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "light or dark theme",
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            ThemeManager.toggleTheme(context)
                        }
                    )
                },
                onClick = {
                    ThemeManager.toggleTheme(context)
                }
            )



            Spacer(modifier = Modifier.height(24.dp))

            // Permissions Section
            Text(
                "Permissions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Location Access
            SettingsItem(
                icon = Icons.Default.LocationOn,
                title = "Location Access",
                subtitle = if (hasLocationPermission.value) "Allowed" else "Not allowed - tap to request",
                onClick = {
                    if (hasLocationPermission.value) {
                        // Already granted
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                }
            )

            // Camera Access
            SettingsItem(
                icon = Icons.Default.CameraAlt,
                title = "Camera Access",
                subtitle = if (hasCameraPermission.value) "Allowed" else "Not allowed - tap to request",
                onClick = {
                    if (hasCameraPermission.value) {
                        // Already granted
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            // Microphone Access
            SettingsItem(
                icon = Icons.Default.Mic,
                title = "Microphone Access",
                subtitle = if (hasMicrophonePermission.value) "Allowed" else "Not allowed - tap to request",
                onClick = {
                    if (hasMicrophonePermission.value) {
                        // Already granted
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            // Story Analysis Section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Story Analysis (Beta)",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Story Analysis
            val storyAnalysisSupported = isStoryAnalysisSupported()
            SettingsItem(
                icon = Icons.Default.InterpreterMode,
                title = "Summarize Stories",
                subtitle = when (storyAnalysisSupported) {
                    true -> "Use on-device AI to summarize stories."
                    false -> "Not supported on this device."
                },
                enabled = storyAnalysisSupported,
                onClick = {
                    when (settings.enableStoryAnalysis) {
                        true -> settingsManager.changeSettings(settings.copy(enableStoryAnalysis = false))
                        false -> showSummarizeStoriesDisclaimer = true
                    }
                },
                trailing = {
                    Switch(
                        checked = settings.enableStoryAnalysis,
                        enabled = isStoryAnalysisSupported(),
                        onCheckedChange = {
                            when (it) {
                                false -> settingsManager.changeSettings(settings.copy(enableStoryAnalysis = false))
                                true -> showSummarizeStoriesDisclaimer = true
                            }
                        }
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.Subtitles,
                title = "Show Summary",
                subtitle = when (settings.enableStoryAnalysis) {
                    true -> "Show the story summary by default."
                    false -> "Story summaries must be enabled."
                },
                enabled = settings.enableStoryAnalysis,
                onClick = {
                    settingsManager.changeSettings(
                        settings.copy(showStorySummaryByDefault = !settings.showStorySummaryByDefault)
                    )
                },
                trailing = {
                    Switch(
                        checked = settings.showStorySummaryByDefault,
                        enabled = settings.enableStoryAnalysis,
                        onCheckedChange = {
                            settingsManager.changeSettings(
                                settings.copy(showStorySummaryByDefault = it)
                            )
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Backup Section
            Text(
                "Backup",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Export backup
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Export backup",
                subtitle = "Save memories to JSON file        ",
                onClick = {
                    exportLauncher.launch(
                        "traverse-backup-${System.currentTimeMillis()}.json"
                    )
                },
                trailing = {
                    TextButton(
                        onClick = {
                            exportLauncher.launch(
                                "traverse-backup-${System.currentTimeMillis()}.json"
                            )
                        },
                    ) {
                        Text("Export",maxLines = 1)

                    }
                }
            )

            // Import backup
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Import backup",
                subtitle = "Restore memories from JSON file",
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                },
                trailing = {
                    TextButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                    ) {
                        Text("Import",maxLines = 1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Section
            Text(
                "Privacy",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Privacy Policy
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = { showPrivacyText = true }
            )

            // Terms of Service
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Terms of Service",
                subtitle = "View terms and conditions",
                onClick = { showTermsText = true }
            )


        }
    }
    if (showPrivacyText) {
        AlertDialog(
            onDismissRequest = { showPrivacyText = false },
            title = { Text("Privacy Policy") },
            text = {
                Column {
                    Text("We respect your privacy.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• We don't share your data")
                    Text("• You control your information")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyText = false }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showTermsText) {
        AlertDialog(
            onDismissRequest = { showTermsText = false },
            title = { Text("Terms of Service") },
            text = {
                Column {
                    Text("By using this app, you agree to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Use the app responsibly")
                    Text("• Not misuse the service")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTermsText = false }
                ) {
                    Text("Agree")
                }
            }
        )
    }
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "This will erase all current memories and stories, " +
                            "then replace them with the data from the backup file. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri
                        showImportConfirm = false
                        pendingImportUri = null

                        if (uri != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val repo = dbState.waitForReady()
                                repo.importBackup(
                                    contentResolver = context.contentResolver,
                                    inputUri = uri
                                )
                            }
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        pendingImportUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    @Suppress("AssignedValueIsNeverRead") // false positive
    if (showSummarizeStoriesDisclaimer) {
        StoryAnalysisDisclaimerDialog(
            onDismissRequest = {
                showSummarizeStoriesDisclaimer = false
            },
            onConfirmation = {
                showSummarizeStoriesDisclaimer = false
                settingsManager.changeSettings(
                    settings.copy(enableStoryAnalysis = true)
                )
            }
        )
    }
}

// Helper function to check if you can use camera
fun canUseCamera(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

// Helper function to check if you can use microphone
fun canUseMicrophone(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = { if (enabled) onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            trailing?.invoke()
        }
    }
}

@Composable
private fun StoryAnalysisDisclaimerDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                Icons.Default.InterpreterMode,
                contentDescription = "AI Icon",
            )
        },
        title = {
            Text(text = "This is a Beta Feature")
        },
        text = {
            Text(text = "It may not work on every device, and it may not generate accurate summaries. You can re-generate summaries from the three-dot menu when viewing a story.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}