package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Converters {

    @TypeConverter
    fun bytesToLatLng(value: ByteArray): LatLng {
        return DataInputStream(value.inputStream()).use {
            readSerializedLatLng(it)
        }
    }

    @TypeConverter
    fun latLngToBytes(value: LatLng): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use {
            writeSerializedLatLng(it, value)
        }
        return out.toByteArray()
    }

    @TypeConverter
    fun uriToString(value: Uri): String {
        return value.toString()
    }

    @TypeConverter
    fun stringToUri(value: String): Uri {
        return Uri.parse(value)
    }

    /**
     * Reads the next byte-serialized LatLng from a DataInputStream.
     * Byte format: [long, long]
     */
    private fun readSerializedLatLng(input: DataInputStream): LatLng {
        return LatLng(input.readDouble(), input.readDouble())
    }

    /**
     * Reads a byte-serialized LatLng to a DataOutputStream.
     * Byte format: [long, long]
     */
    private fun writeSerializedLatLng(output: DataOutputStream, latLng: LatLng) {
        output.writeDouble(latLng.latitude)
        output.writeDouble(latLng.longitude)
    }
}