package mirujam.nekomemo.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Converters @Inject constructor() {

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
