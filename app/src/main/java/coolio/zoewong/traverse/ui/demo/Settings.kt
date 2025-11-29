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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode = ThemeManager.isDarkMode
    var notifications by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("English") }
    var cameraPermission by remember { mutableStateOf(false) }
    var microphonePermission by remember { mutableStateOf(false) }
    val dbState = DatabaseState.current

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
                }
            )

            // Notifications
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Enable or disable push notifications",
                trailing = {
                    Switch(
                        checked = notifications,
                        onCheckedChange = { notifications = it }
                    )
                }
            )

            // Language
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = language,
                onClick = {
                    // TODO: actual language switching if time permits
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions Section
            Text(
                "Permissions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Camera Access
            SettingsItem(
                icon = Icons.Default.CameraAlt,
                title = "Camera Access",
                subtitle = if (cameraPermission) "Allowed" else "Not allowed",
                onClick = {
                    // TODO: Request camera permission or open app settings?
                    cameraPermission = !cameraPermission
                }
            )

            // Microphone Access
            SettingsItem(
                icon = Icons.Default.Mic,
                title = "Microphone Access",
                subtitle = if (microphonePermission) "Allowed" else "Not allowed",
                onClick = {
                    // TODO: Request microphone permission or open app settings?
                    microphonePermission = !microphonePermission
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
                subtitle = "Save memories & stories to a JSON file",
                onClick = null,
                trailing = {
                    TextButton(
                        onClick = {
                            exportLauncher.launch(
                                "traverse-backup-${System.currentTimeMillis()}.json"
                            )
                        }
                    ) {
                        Text("Export")
                    }
                }
            )

            // Import backup
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Import backup",
                subtitle = "Restore memories & stories from a JSON file",
                onClick = null,
                trailing = {
                    TextButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    ) {
                        Text("Import")
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
                onClick = {
                    // TODO: Open privacy policy
                }
            )

            // Terms of Service
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Terms of Service",
                subtitle = "View terms and conditions",
                onClick = {
                    // TODO: Open terms of service
                }
            )


        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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