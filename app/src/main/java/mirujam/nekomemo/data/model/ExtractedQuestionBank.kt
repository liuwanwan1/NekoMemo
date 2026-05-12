package mirujam.nekomemo.data.model

data class ExtractedQuestion(
    val type: String,
    val content: String,
    val options: List<String>,
    val correctAnswer: String,
    val correctIndex: Int
)

data class ExtractedQuestionBank(
    val name: String,
    val questions: List<ExtractedQuestion>,
    val skippedCount: Int = 0,
    val unsupportedTypeCount: Int = 0
) {
    fun toJson(): String {
        val json = org.json.JSONObject()
        json.put("name", name)
        json.put("skippedCount", skippedCount)
        json.put("unsupportedTypeCount", unsupportedTypeCount)
        val questionsArray = org.json.JSONArray()
        questions.forEach { q ->
            val qJson = org.json.JSONObject()
            qJson.put("type", q.type)
            qJson.put("content", q.content)
            qJson.put("options", org.json.JSONArray(q.options))
            qJson.put("correctAnswer", q.correctAnswer)
            qJson.put("correctIndex", q.correctIndex)
            questionsArray.put(qJson)
        }
        json.put("questions", questionsArray)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): ExtractedQuestionBank? {
            return try {
                val json = org.json.JSONObject(jsonString)
                val name = json.optString("name", "Untitled Bank")
                val skippedCount = json.optInt("skippedCount", 0)
                val unsupportedTypeCount = json.optInt("unsupportedTypeCount", 0)
                val questionsArray = json.optJSONArray("questions") ?: return ExtractedQuestionBank(name, emptyList(), skippedCount, unsupportedTypeCount)
                val questions = (0 until questionsArray.length()).map { i ->
                    val qJson = questionsArray.getJSONObject(i)
                    val optionsArray = qJson.optJSONArray("options")
                    val options = if (optionsArray != null) {
                        (0 until optionsArray.length()).map { j -> optionsArray.getString(j) }
                    } else {
                        emptyList()
                    }
                    ExtractedQuestion(
                        type = qJson.optString("type", "Unknown"),
                        content = qJson.optString("content", ""),
                        options = options,
                        correctAnswer = qJson.optString("correctAnswer", ""),
                        correctIndex = qJson.optInt("correctIndex", 0)
                    )
                }
                ExtractedQuestionBank(name, questions, skippedCount, unsupportedTypeCount)
            } catch (_: Exception) {
                null
            }
        }
    }
}
