package mirujam.nekomemo.data.local

import org.json.JSONArray
import timber.log.Timber

object ListJsonConverter {

    fun fromStringList(value: List<String>): String {
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse options JSON")
            emptyList()
        }
    }

    fun fromIntList(value: List<Int>): String {
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    fun toIntList(value: String): List<Int> {
        if (value.isBlank() || value == "[]") return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getInt(it) }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse int list JSON")
            emptyList()
        }
    }
}
