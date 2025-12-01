package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Converters {

    // ---------- LatLng <-> ByteArray ----------

    @TypeConverter
    fun bytesToLatLng(value: ByteArray?): LatLng? {
        if (value == null) return null
        return DataInputStream(value.inputStream()).use {
            readSerializedLatLng(it)
        }
    }

    @TypeConverter
    fun latLngToBytes(value: LatLng?): ByteArray? {
        if (value == null) return null
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use {
            writeSerializedLatLng(it, value)
        }
        return out.toByteArray()
    }

    // ---------- Uri <-> String ----------

    @TypeConverter
    fun uriToString(value: Uri?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun stringToUri(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    /**
     * Reads the next byte-serialized LatLng from a DataInputStream.
     * Byte format: [double, double]
     */
    private fun readSerializedLatLng(input: DataInputStream): LatLng {
        return LatLng(input.readDouble(), input.readDouble())
    }

    /**
     * Writes a byte-serialized LatLng to a DataOutputStream.
     * Byte format: [double, double]
     */
    private fun writeSerializedLatLng(output: DataOutputStream, latLng: LatLng) {
        output.writeDouble(latLng.latitude)
        output.writeDouble(latLng.longitude)
    }
}
