package mirujam.nekomemo.ui.fetcher

import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject

data class ExtractedQuestion(
    val type: String,
    val content: String,
    val options: List<String>,
    val correctAnswer: String,
    val correctIndex: Int
)

data class ExtractedQuestionBank(
    val name: String,
    val questions: List<ExtractedQuestion>
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("name", name)
        val questionsArray = JSONArray()
        questions.forEach { q ->
            val qJson = JSONObject()
            qJson.put("type", q.type)
            qJson.put("content", q.content)
            qJson.put("options", JSONArray(q.options))
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
                val json = JSONObject(jsonString)
                val name = json.optString("name", "Untitled Bank")
                val questionsArray = json.optJSONArray("questions") ?: return ExtractedQuestionBank(name, emptyList())
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
                ExtractedQuestionBank(name, questions)
            } catch (_: Exception) {
                null
            }
        }
    }
}

object HtmlParser {

    fun parse(html: String): ExtractedQuestionBank {
        val doc = Jsoup.parse(html)

        val bankName = doc.select("h2.mark_title").text().trim()

        val questions = mutableListOf<ExtractedQuestion>()
        val questionDivs = doc.select("div.questionLi")

        for (div in questionDivs) {
            val type = parseQuestionType(div)
            val content = parseQuestionContent(div)
            val options = parseOptions(div)
            val correctAnswer = parseCorrectAnswer(div)
            val correctIndex = letterToIndex(correctAnswer)

            if (content.isNotBlank() && options.isNotEmpty()) {
                questions.add(
                    ExtractedQuestion(
                        type = type,
                        content = content,
                        options = options,
                        correctAnswer = correctAnswer,
                        correctIndex = correctIndex
                    )
                )
            }
        }

        return ExtractedQuestionBank(
            name = bankName,
            questions = questions
        )
    }

    private fun parseQuestionType(div: org.jsoup.nodes.Element): String {
        val typeSpan = div.select("span.colorShallow").first()
        val typeText = typeSpan?.text()?.trim() ?: return "Unknown"
        return when {
            typeText.contains("单选题") -> "Single Choice"
            typeText.contains("多选题") -> "Multiple Choice"
            typeText.contains("判断题") -> "True/False"
            typeText.contains("填空题") -> "Fill in the Blank"
            typeText.contains("简答题") -> "Short Answer"
            else -> "Unknown"
        }
    }

    private fun parseQuestionContent(div: org.jsoup.nodes.Element): String {
        val contentSpan = div.select("span.qtContent").first()
        if (contentSpan != null) {
            return contentSpan.text().trim()
        }

        val h3 = div.select("h3.mark_name").first() ?: return ""
        val typeSpan = h3.select("span.colorShallow").first()
        if (typeSpan != null) {
            val fullText = h3.text().trim()
            val typeText = typeSpan.text().trim()
            val contentStart = fullText.indexOf(typeText)
            if (contentStart >= 0) {
                val afterType = fullText.substring(contentStart + typeText.length).trim()
                val numberPrefix = Regex("^\\d+\\.\\s*")
                return numberPrefix.replace(afterType, "").trim()
            }
        }
        return h3.text().trim()
    }

    private fun parseOptions(div: org.jsoup.nodes.Element): List<String> {
        val optionLis = div.select("ul.mark_letter li")
        if (optionLis.isNotEmpty()) {
            return optionLis.map { li -> li.text().trim() }.filter { it.isNotBlank() }
        }

        val answerDivs = div.select("div.stem_answer div.answerBg")
        if (answerDivs.isNotEmpty()) {
            return answerDivs.mapNotNull { answerDiv ->
                val letterSpan = answerDiv.select("span.num_option").first()
                val textDiv = answerDiv.select("div.answer_p").first()
                val letter = letterSpan?.text()?.trim() ?: ""
                val text = textDiv?.text()?.trim() ?: ""
                if (letter.isNotBlank() && text.isNotBlank()) {
                    "$letter. $text"
                } else if (text.isNotBlank()) {
                    text
                } else null
            }
        }

        return emptyList()
    }

    private fun parseCorrectAnswer(div: org.jsoup.nodes.Element): String {
        val correctSpan = div.select("span.rightAnswerContent").first()
        if (correctSpan != null) {
            return correctSpan.text().trim()
        }

        val greenSpan = div.select("span.colorGreen").first()
        if (greenSpan != null) {
            val text = greenSpan.text().trim()
            val match = Regex("正确答案[:\\s]*([A-Ha-h])").find(text)
            if (match != null) {
                return match.groupValues[1].uppercase()
            }
            val letterMatch = Regex("[A-Ha-h]").find(text)
            if (letterMatch != null) {
                return letterMatch.value.uppercase()
            }
        }

        return ""
    }

    private fun letterToIndex(letter: String): Int {
        if (letter.isBlank()) return 0
        val letters = "ABCDEFGH"
        val index = letters.indexOf(letter.uppercase())
        return if (index >= 0) index else 0
    }
}
