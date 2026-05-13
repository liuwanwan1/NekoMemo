package mirujam.nekomemo.domain.validator

import android.util.Log

object DataValidator {
    private const val TAG = "DataValidator"

    const val MAX_TITLE_LENGTH = 200
    const val MAX_CATEGORY_LENGTH = 100
    const val MAX_TEXT_LENGTH = 10000
    const val MAX_OPTION_LENGTH = 2000
    const val MAX_OPTIONS_COUNT = 10
    const val MAX_QUESTIONS_COUNT = 5000
    const val MIN_OPTIONS_COUNT = 2
    const val MAX_JSON_SIZE = 10 * 1024 * 1024 // 10MB

    fun sanitizeString(input: String, maxLength: Int, defaultValue: String = ""): String {
        if (input.isBlank()) return defaultValue

        val trimmed = input.trim()

        return if (trimmed.length > maxLength) {
            Log.d(TAG, "String truncated from ${trimmed.length} to $maxLength characters")
            trimmed.take(maxLength)
        } else {
            trimmed
        }
    }

    fun sanitizeContent(content: String): String {
        return if (content.length > MAX_TEXT_LENGTH) {
            Log.w(TAG, "Content truncated from ${content.length} to $MAX_TEXT_LENGTH")
            content.take(MAX_TEXT_LENGTH)
        } else {
            content
        }
    }

    fun sanitizeOption(option: String): String {
        return if (option.length > MAX_OPTION_LENGTH) {
            option.take(MAX_OPTION_LENGTH)
        } else {
            option
        }
    }

    fun sanitizeOptions(options: List<String>): List<String> {
        return options
            .take(MAX_OPTIONS_COUNT)
            .map { it.trim().take(MAX_OPTION_LENGTH) }
            .filter { it.isNotBlank() }
    }

    fun validateCorrectIndex(index: Int, options: List<String>): Int {
        return if (options.isEmpty()) {
            0
        } else {
            index.coerceIn(0, options.size - 1)
        }
    }

    fun validateTitle(title: String): String {
        return sanitizeString(title, MAX_TITLE_LENGTH, "Untitled Bank").ifBlank { "Untitled Bank" }
    }

    fun validateCategory(category: String): String {
        return sanitizeString(category, MAX_CATEGORY_LENGTH, "General").ifBlank { "General" }
    }
}
