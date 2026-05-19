package mirujam.nekomemo.data.local

import org.json.JSONArray

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
        } catch (_: Exception) {
            emptyList()
        }
    }
}
