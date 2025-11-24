package coolio.zoewong.traverse.util

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

object ImageLocationExtractor {
    
    suspend fun extractLocation(context: Context, imageUri: Uri): LatLng? {
        return try {
            withContext(Dispatchers.IO) {
                val uriString = imageUri.toString()
                if (uriString.contains("picker_get_content") || uriString.contains("photopicker")) {
                    return@withContext null
                }
                
                try {
                    val inputStream: InputStream? = try {
                        context.contentResolver.openInputStream(imageUri)
                    } catch (e: SecurityException) {
                        return@withContext null
                    } catch (e: Exception) {
                        null
                    }
                    
                    inputStream?.use { stream ->
                        try {
                            val exif = ExifInterface(stream)
                            
                            val latLong = FloatArray(2)
                            val hasLocation = exif.getLatLong(latLong)
                            
                            if (hasLocation) {
                                LatLng(latLong[0].toDouble(), latLong[1].toDouble())
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                } catch (e: SecurityException) {
                    null
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getLocationName(context: Context, latLng: LatLng): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext null
                }
                
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val locationName = when {
                        address.locality != null -> {
                            if (address.adminArea != null) {
                                "${address.locality}, ${address.adminArea}"
                            } else {
                                address.locality
                            }
                        }
                        address.adminArea != null -> address.adminArea
                        address.countryName != null -> address.countryName
                        else -> null
                    }
                    locationName
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("ImageLocationExtractor", "Error reverse geocoding location", e)
                null
            }
        }
    }
}

