package mirujam.nekomemo.data.model

import mirujam.nekomemo.domain.validator.DataValidator

data class ExtractedQuestion(
    val type: String,
    val content: String,
    val options: List<String>,
    val correctAnswer: String,
    val correctIndex: Int
) {
    companion object {
        fun sanitizeContent(content: String): String = DataValidator.sanitizeContent(content)

        fun sanitizeOption(option: String): String = DataValidator.sanitizeOption(option)

        fun sanitizeOptions(options: List<String>): List<String> = DataValidator.sanitizeOptions(options)

        fun validateCorrectIndex(correctIndex: Int, options: List<String>): Int =
            DataValidator.validateCorrectIndex(correctIndex, options)
    }
}

data class ExtractedQuestionBank(
    val name: String,
    val questions: List<ExtractedQuestion>,
    val skippedCount: Int = 0,
    val unsupportedTypeCount: Int = 0
) {
    fun toJson(): String {
        return try {
            val json = org.json.JSONObject()
            
            json.put("name", name.ifBlank { "Untitled Bank" })
            json.put("skippedCount", skippedCount.coerceAtLeast(0))
            json.put("unsupportedTypeCount", unsupportedTypeCount.coerceAtLeast(0))
            
            val questionsArray = org.json.JSONArray()
            
            questions.forEachIndexed { index, q ->
                try {
                    val qJson = org.json.JSONObject()
                    qJson.put("type", q.type.ifBlank { "Unknown" })
                    qJson.put("content", ExtractedQuestion.sanitizeContent(q.content))
                    
                    val sanitizedOptions = ExtractedQuestion.sanitizeOptions(q.options)
                    qJson.put("options", org.json.JSONArray(sanitizedOptions))
                    qJson.put("correctAnswer", q.correctAnswer)
                    qJson.put("correctIndex", ExtractedQuestion.validateCorrectIndex(q.correctIndex, sanitizedOptions))
                    
                    questionsArray.put(qJson)
                } catch (e: Exception) {
                    android.util.Log.e("ExtractedQuestionBank", "toJson: Error serializing question at index $index", e)
                    
                    try {
                        val fallbackJson = org.json.JSONObject()
                        fallbackJson.put("type", "Error")
                        fallbackJson.put("content", "Failed to serialize question")
                        fallbackJson.put("options", org.json.JSONArray(emptyList<String>()))
                        fallbackJson.put("correctAnswer", "")
                        fallbackJson.put("correctIndex", 0)
                        questionsArray.put(fallbackJson)
                    } catch (e2: Exception) {
                        android.util.Log.e("ExtractedQuestionBank", "toJson: Failed to create fallback question at index $index", e2)
                    }
                }
            }
            
            json.put("questions", questionsArray)
            val result = json.toString()
            
            android.util.Log.d("ExtractedQuestionBank", "toJson: Successfully serialized ${questions.size} questions, output size=${result.length}")
            result
            
        } catch (e: Exception) {
            android.util.Log.e("ExtractedQuestionBank", "toJson: Fatal error during serialization", e)
            
            try {
                val fallbackJson = org.json.JSONObject()
                fallbackJson.put("name", name.ifBlank { "Untitled Bank" })
                fallbackJson.put("skippedCount", skippedCount)
                fallbackJson.put("unsupportedTypeCount", unsupportedTypeCount + questions.size)
                fallbackJson.put("questions", org.json.JSONArray())
                
                android.util.Log.w("ExtractedQuestionBank", "toJson: Returning minimal JSON due to serialization error")
                fallbackJson.toString()
            } catch (e2: Exception) {
                android.util.Log.e("ExtractedQuestionBank", "toJson: Even fallback failed", e2)
                "{\"name\":\"Error\",\"questions\":[]}"
            }
        }
    }

    companion object {
        private const val TAG = "ExtractedQuestionBank"
        
        fun fromJson(jsonString: String): ExtractedQuestionBank? {
            if (jsonString.isBlank()) {
                android.util.Log.w(TAG, "fromJson: Input string is blank")
                return null
            }
            
            if (jsonString.length > 50 * 1024 * 1024) { // 50MB limit
                android.util.Log.e(TAG, "fromJson: Input size ${jsonString.length} exceeds 50MB limit")
                return null
            }
            
            return try {
                val json = org.json.JSONObject(jsonString)
                
                val name = json.optString("name", "Untitled Bank").ifBlank { "Untitled Bank" }
                val skippedCount = json.optInt("skippedCount", 0).coerceAtLeast(0)
                val unsupportedTypeCount = json.optInt("unsupportedTypeCount", 0).coerceAtLeast(0)
                
                val questionsArray = json.optJSONArray("questions")
                
                if (questionsArray == null) {
                    android.util.Log.d(TAG, "fromJson: No 'questions' array found, creating empty bank with name='$name'")
                    return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount)
                }
                
                if (questionsArray.length() == 0) {
                    android.util.Log.d(TAG, "fromJson: Empty questions array for bank '$name'")
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
                                    android.util.Log.w(TAG, "fromJson: Failed to read option $j in question $i", e)
                                    null
                                }
                            }.filter { it.isNotBlank() }
                        } else {
                            emptyList()
                        }
                        
                        val sanitizedOptions = ExtractedQuestion.sanitizeOptions(rawOptions)
                        val content = ExtractedQuestion.sanitizeContent(qJson.optString("content", ""))
                        
                        if (content.isBlank()) {
                            android.util.Log.w(TAG, "fromJson: Skipping question $i due to blank content")
                            parseErrors++
                            continue
                        }
                        
                        if (sanitizedOptions.isEmpty()) {
                            android.util.Log.w(TAG, "fromJson: Skipping question $i due to no valid options")
                            parseErrors++
                            continue
                        }
                        
                        val question = ExtractedQuestion(
                            type = qJson.optString("type", "Unknown").ifBlank { "Unknown" },
                            content = content,
                            options = sanitizedOptions,
                            correctAnswer = qJson.optString("correctAnswer", ""),
                            correctIndex = ExtractedQuestion.validateCorrectIndex(
                                qJson.optInt("correctIndex", 0),
                                sanitizedOptions
                            )
                        )
                        
                        validQuestions.add(question)
                        
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "fromJson: Failed to parse question at index $i", e)
                        parseErrors++
                    }
                }
                
                if (parseErrors > 0) {
                    android.util.Log.w(TAG, "fromJson: Completed with $parseErrors parsing errors out of ${questionsArray.length()} total. Valid questions: ${validQuestions.size}")
                }
                
                if (validQuestions.isEmpty() && questionsArray.length() > 0) {
                    android.util.Log.e(TAG, "fromJson: All ${questionsArray.length()} questions failed to parse!")
                    return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount + questionsArray.length())
                }
                
                val result = ExtractedQuestionBank(name, validQuestions, skippedCount, unsupportedTypeCount)
                android.util.Log.d(TAG, "fromJson: Successfully parsed ${validQuestions.size}/${questionsArray.length()} questions for bank '$name'")
                result
                
            } catch (e: org.json.JSONException) {
                android.util.Log.e(TAG, "fromJson: JSON parsing error (input length=${jsonString.length}, preview=${jsonString.take(200)})", e)
                null
            } catch (e: Exception) {
                android.util.Log.e(TAG, "fromJson: Unexpected error (input length=${jsonString.length})", e)
                null
            }
        }
        
        fun fromJsonSafe(jsonString: String): ExtractedQuestionBank {
            return fromJson(jsonString) ?: run {
                android.util.Log.w(TAG, "fromJsonSafe: Parsing failed, returning empty bank")
                ExtractedQuestionBank("Imported Bank (Partial)", emptyList())
            }
        }
    }
}
