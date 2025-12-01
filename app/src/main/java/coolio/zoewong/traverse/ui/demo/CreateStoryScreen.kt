package coolio.zoewong.traverse.ui.demo

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onCancel: () -> Unit,
    onCreate: (String, String?, Uri?, LatLng?) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var pickedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isResolvingLocation by remember { mutableStateOf(false) }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            coverUri = uri
        }
    )


    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {

            fetchCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onStart = { isResolvingLocation = true },
                onSuccess = { latLng ->
                    pickedLatLng = latLng

                    if (locationName.isBlank() && latLng != null) {
                        locationName = "(${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})"
                    }
                    isResolvingLocation = false
                    Toast.makeText(context, "Location captured", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    isResolvingLocation = false
                    Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(
                context,
                "Location permission is required to use current location",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun tryUseCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fetchCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onStart = { isResolvingLocation = true },
                onSuccess = { latLng ->
                    pickedLatLng = latLng
                    if (locationName.isBlank() && latLng != null) {
                        locationName = "(${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})"
                    }
                    isResolvingLocation = false
                    Toast.makeText(context, "Location captured", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    isResolvingLocation = false
                    Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Story title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Location name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { if (!isResolvingLocation) tryUseCurrentLocation() }) {
                    if (isResolvingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Use current location")
                }
                if (pickedLatLng != null) {
                    Text(
                        text = "(${pickedLatLng!!.latitude.format(4)}, ${pickedLatLng!!.longitude.format(4)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

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

            OutlinedButton(
                onClick = { pickCoverLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (coverUri == null) "Choose from gallery" else "Change cover")
            }

            Spacer(Modifier.height(8.dp))

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
                                locationName.ifBlank { null },
                                coverUri,
                                pickedLatLng      // ⭐ 把坐标传出去
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


private fun Double.format(decimals: Int): String =
    "%.${decimals}f".format(this)


@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun fetchCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onStart: () -> Unit,
    onSuccess: (LatLng?) -> Unit,
    onError: () -> Unit
) {
    onStart()
    fusedLocationClient.lastLocation
        .addOnSuccessListener { loc ->
            if (loc != null) {
                onSuccess(LatLng(loc.latitude, loc.longitude))
            } else {
                onSuccess(null)
            }
        }
        .addOnFailureListener {
            onError()
        }
}
