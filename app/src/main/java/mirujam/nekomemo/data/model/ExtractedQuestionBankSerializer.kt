package mirujam.nekomemo.data.model

import android.util.Log
import mirujam.nekomemo.domain.validator.DataValidator

object ExtractedQuestionBankSerializer {

    private const val TAG = "ExtractedQuestionBankSerializer"

    fun toJson(bank: ExtractedQuestionBank): String {
        return try {
            val json = org.json.JSONObject()

            json.put("name", bank.name.ifBlank { "Untitled Bank" })
            json.put("skippedCount", bank.skippedCount.coerceAtLeast(0))
            json.put("unsupportedTypeCount", bank.unsupportedTypeCount.coerceAtLeast(0))

            val questionsArray = org.json.JSONArray()

            bank.questions.forEachIndexed { index, q ->
                try {
                    val qJson = org.json.JSONObject()
                    qJson.put("type", q.type.ifBlank { "Unknown" })
                    qJson.put("content", DataValidator.sanitizeContent(q.content))

                    val sanitizedOptions = DataValidator.sanitizeOptions(q.options)
                    qJson.put("options", org.json.JSONArray(sanitizedOptions))
                    qJson.put("correctAnswer", q.correctAnswer)
                    qJson.put("correctIndex", DataValidator.validateCorrectIndex(q.correctIndex, sanitizedOptions))

                    questionsArray.put(qJson)
                } catch (e: Exception) {
                    Log.e(TAG, "toJson: Error serializing question at index $index", e)

                    try {
                        val fallbackJson = org.json.JSONObject()
                        fallbackJson.put("type", "Error")
                        fallbackJson.put("content", "Failed to serialize question")
                        fallbackJson.put("options", org.json.JSONArray(emptyList<String>()))
                        fallbackJson.put("correctAnswer", "")
                        fallbackJson.put("correctIndex", 0)
                        questionsArray.put(fallbackJson)
                    } catch (e2: Exception) {
                        Log.e(TAG, "toJson: Failed to create fallback question at index $index", e2)
                    }
                }
            }

            json.put("questions", questionsArray)
            val result = json.toString()

            Log.d(TAG, "toJson: Successfully serialized ${bank.questions.size} questions, output size=${result.length}")
            result

        } catch (e: Exception) {
            Log.e(TAG, "toJson: Fatal error during serialization", e)

            try {
                val fallbackJson = org.json.JSONObject()
                fallbackJson.put("name", bank.name.ifBlank { "Untitled Bank" })
                fallbackJson.put("skippedCount", bank.skippedCount)
                fallbackJson.put("unsupportedTypeCount", bank.unsupportedTypeCount + bank.questions.size)
                fallbackJson.put("questions", org.json.JSONArray())

                Log.w(TAG, "toJson: Returning minimal JSON due to serialization error")
                fallbackJson.toString()
            } catch (e2: Exception) {
                Log.e(TAG, "toJson: Even fallback failed", e2)
                "{\"name\":\"Error\",\"questions\":[]}"
            }
        }
    }

    fun fromJson(jsonString: String): ExtractedQuestionBank? {
        if (jsonString.isBlank()) {
            Log.w(TAG, "fromJson: Input string is blank")
            return null
        }

        if (jsonString.length > 50 * 1024 * 1024) {
            Log.e(TAG, "fromJson: Input size ${jsonString.length} exceeds 50MB limit")
            return null
        }

        return try {
            val json = org.json.JSONObject(jsonString)

            val name = json.optString("name", "Untitled Bank").ifBlank { "Untitled Bank" }
            val skippedCount = json.optInt("skippedCount", 0).coerceAtLeast(0)
            val unsupportedTypeCount = json.optInt("unsupportedTypeCount", 0).coerceAtLeast(0)

            val questionsArray = json.optJSONArray("questions")

            if (questionsArray == null) {
                Log.d(TAG, "fromJson: No 'questions' array found, creating empty bank with name='$name'")
                return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount)
            }

            if (questionsArray.length() == 0) {
                Log.d(TAG, "fromJson: Empty questions array for bank '$name'")
                return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount)
            }

            val validQuestions = mutableListOf<ExtractedQuestion>()
            var parseErrors = 0

            for (i in 0 until questionsArray.length()) {
                try {
                    val qJson = questionsArray.getJSONObject(i)

                    val rawOptionsArray = qJson.optJSONArray("options")
                    val rawOptions = if (rawOptionsArray != null) {
                        (0 until rawOptionsArray.length()).mapNotNull { j ->
                            try {
                                rawOptionsArray.getString(j)
                            } catch (e: Exception) {
                                Log.w(TAG, "fromJson: Failed to read option $j in question $i", e)
                                null
                            }
                        }.filter { it.isNotBlank() }
                    } else {
                        emptyList()
                    }

                    val sanitizedOptions = DataValidator.sanitizeOptions(rawOptions)
                    val content = DataValidator.sanitizeContent(qJson.optString("content", ""))

                    if (content.isBlank()) {
                        Log.w(TAG, "fromJson: Skipping question $i due to blank content")
                        parseErrors++
                        continue
                    }

                    if (sanitizedOptions.isEmpty()) {
                        Log.w(TAG, "fromJson: Skipping question $i due to no valid options")
                        parseErrors++
                        continue
                    }

                    val question = ExtractedQuestion(
                        type = qJson.optString("type", "Unknown").ifBlank { "Unknown" },
                        content = content,
                        options = sanitizedOptions,
                        correctAnswer = qJson.optString("correctAnswer", ""),
                        correctIndex = DataValidator.validateCorrectIndex(
                            qJson.optInt("correctIndex", 0),
                            sanitizedOptions
                        )
                    )

                    validQuestions.add(question)

                } catch (e: Exception) {
                    Log.e(TAG, "fromJson: Failed to parse question at index $i", e)
                    parseErrors++
                }
            }

            if (parseErrors > 0) {
                Log.w(TAG, "fromJson: Completed with $parseErrors parsing errors out of ${questionsArray.length()} total. Valid questions: ${validQuestions.size}")
            }

            if (validQuestions.isEmpty() && questionsArray.length() > 0) {
                Log.e(TAG, "fromJson: All ${questionsArray.length()} questions failed to parse!")
                return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount + questionsArray.length())
            }

            val result = ExtractedQuestionBank(name, validQuestions, skippedCount, unsupportedTypeCount)
            Log.d(TAG, "fromJson: Successfully parsed ${validQuestions.size}/${questionsArray.length()} questions for bank '$name'")
            result

        } catch (e: org.json.JSONException) {
            Log.e(TAG, "fromJson: JSON parsing error (input length=${jsonString.length}, preview=${jsonString.take(200)})", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fromJson: Unexpected error (input length=${jsonString.length})", e)
            null
        }
    }
}
