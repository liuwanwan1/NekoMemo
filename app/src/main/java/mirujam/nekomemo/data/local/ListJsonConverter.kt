package mirujam.nekomemo.data.local

import timber.log.Timber

/**
 * Manual JSON array serialization/deserialization to avoid dependency on org.json
 * which returns default values in unit tests when isReturnDefaultValues is enabled.
 */
object ListJsonConverter {

    fun fromStringList(value: List<String>): String {
        if (value.isEmpty()) return "[]"
        return value.joinToString(",", "[", "]") { "\"${it.escapeJson()}\"" }
    }

    fun toStringList(value: String): List<String> {
        if (value.isBlank() || value == "[]") return emptyList()
        return try {
            parseJsonArray(value)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse options JSON: $value")
            emptyList()
        }
    }

    fun fromIntList(value: List<Int>): String {
        if (value.isEmpty()) return "[]"
        return value.joinToString(",", "[", "]")
    }

    fun toIntList(value: String): List<Int> {
        if (value.isBlank() || value == "[]") return emptyList()
        return try {
            val trimmed = value.trim()
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                Timber.w("Invalid int list JSON format: $value")
                return emptyList()
            }
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return emptyList()
            inner.split(",").mapNotNull { it.trim().toIntOrNull() }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse int list JSON: $value")
            emptyList()
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw IllegalArgumentException("Not a JSON array: $json")
        }
        val inner = trimmed.substring(1, trimmed.length - 1)
        if (inner.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        var i = 0
        while (i < inner.length) {
            // Skip whitespace and commas
            while (i < inner.length && (inner[i].isWhitespace() || inner[i] == ',')) {
                i++
            }
            if (i >= inner.length) break

            if (inner[i] == '"') {
                // Parse quoted string
                i++ // skip opening quote
                val sb = StringBuilder()
                while (i < inner.length && inner[i] != '"') {
                    if (inner[i] == '\\' && i + 1 < inner.length) {
                        when (inner[i + 1]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            else -> sb.append(inner[i + 1])
                        }
                        i += 2
                    } else {
                        sb.append(inner[i])
                        i++
                    }
                }
                if (i < inner.length && inner[i] == '"') {
                    i++ // skip closing quote
                }
                result.add(sb.toString())
            } else {
                // Parse unquoted value (skip until comma or end)
                val start = i
                while (i < inner.length && inner[i] != ',') {
                    i++
                }
                result.add(inner.substring(start, i).trim())
            }
        }
        return result
    }

    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
    }
}
