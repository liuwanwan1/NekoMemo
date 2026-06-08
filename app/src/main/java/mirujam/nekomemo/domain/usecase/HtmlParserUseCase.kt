package mirujam.nekomemo.domain.usecase

import mirujam.nekomemo.domain.model.ExtractedQuestion
import mirujam.nekomemo.domain.model.ExtractedQuestionBank
import mirujam.nekomemo.domain.model.QuestionType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

import javax.inject.Inject

class HtmlParserUseCase @Inject constructor() : mirujam.nekomemo.domain.parser.HtmlParser {

    companion object {
        private val NUMBER_PREFIX_REGEX = Regex("^\\d+\\.\\s*")
        private val LETTER_PREFIX_REGEX = Regex("^[A-Ha-h]\\.\\s*")
        private val CORRECT_ANSWER_REGEX = Regex("正确答案[:\\s]*([A-Ha-h]+)")
        private val LETTER_REGEX = Regex("[A-Ha-h]+")
    }

    override fun parse(html: String): ExtractedQuestionBank {
        Timber.d("Starting parse, HTML length: ${html.length}")
        val startTime = System.currentTimeMillis()

        val doc: Document = try {
            Jsoup.parse(html)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse HTML")
            return ExtractedQuestionBank("", emptyList())
        }

        val bankName = doc.select("h2.mark_title").text().trim()
        Timber.d("Bank name: '$bankName'")

        val questionDivs = doc.select("div.questionLi")
        val totalQuestions = questionDivs.size
        Timber.d("Found $totalQuestions question div(s)")

        if (totalQuestions == 0) {
            Timber.w("No questions found!")
            return ExtractedQuestionBank(bankName, emptyList())
        }

        val questions = mutableListOf<ExtractedQuestion>()
        var skippedCount = 0
        var unsupportedTypeCount = 0
        var processedCount = 0

        for ((index, div) in questionDivs.withIndex()) {
            try {
                val type = parseQuestionType(div)
                val questionType = mapQuestionType(type)

                if (questionType == null) {
                    Timber.d("Skipping question $index: unsupported type '$type'")
                    unsupportedTypeCount++
                    continue
                }

                val content = ExtractedQuestion.sanitizeContent(parseQuestionContent(div))

                val options: List<String>
                val correctAnswer: String
                val correctIndices: List<Int>

                if (questionType == QuestionType.TRUE_FALSE) {
                    // True/False: always two options
                    options = listOf("对", "错")
                    correctAnswer = parseCorrectAnswer(div)
                    val answerIndex = letterToIndex(correctAnswer)
                    correctIndices = listOf(answerIndex.coerceIn(0, 1))
                } else {
                    options = parseOptions(div)
                    correctAnswer = parseCorrectAnswer(div)
                    correctIndices = lettersToIndices(correctAnswer)
                }

                if (content.isNotBlank() && options.isNotEmpty() && correctAnswer.isNotBlank()) {
                    questions.add(
                        ExtractedQuestion(
                            type = type,
                            content = content,
                            questionType = questionType,
                            options = options,
                            correctAnswer = correctAnswer,
                            correctIndex = correctIndices.first(),
                            correctIndices = correctIndices
                        )
                    )
                    processedCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                Timber.w("Error parsing question $index: ${e.message}")
                skippedCount++
            }

            if (index % 50 == 0 && index > 0) {
                Timber.d("Progress: $index/$totalQuestions processed")
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Timber.d("Parse complete in ${elapsed}ms. Valid: $processedCount/$totalQuestions, Skipped (no answer): $skippedCount, Unsupported type: $unsupportedTypeCount")

        return ExtractedQuestionBank(
            name = bankName,
            questions = questions,
            skippedCount = skippedCount,
            unsupportedTypeCount = unsupportedTypeCount
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
                return NUMBER_PREFIX_REGEX.replace(afterType, "").trim()
            }
        }
        return h3.text().trim()
    }

    private fun parseOptions(div: org.jsoup.nodes.Element): List<String> {
        val optionLis = div.select("ul.mark_letter li")
        if (optionLis.isNotEmpty()) {
            return optionLis.mapNotNull { li ->
                val text = li.text().trim()
                val cleanText = LETTER_PREFIX_REGEX.replace(text, "")
                cleanText.takeIf { it.isNotBlank() }
            }
        }

        val answerDivs = div.select("div.stem_answer div.answerBg")
        if (answerDivs.isNotEmpty()) {
            return answerDivs.mapNotNull { answerDiv ->
                try {
                    val textDiv = answerDiv.select("div.answer_p").first()
                    val text = textDiv?.text()?.trim() ?: ""
                    val cleanText = LETTER_PREFIX_REGEX.replace(text, "")
                    cleanText.takeIf { it.isNotBlank() }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse answer div")
                    null
                }
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
            val match = CORRECT_ANSWER_REGEX.find(text)
            if (match != null) {
                return match.groupValues[1].uppercase()
            }
            val letterMatch = LETTER_REGEX.find(text)
            if (letterMatch != null) {
                return letterMatch.value.uppercase()
            }
        }

        return ""
    }

    private fun mapQuestionType(type: String): QuestionType? = when (type) {
        "Single Choice" -> QuestionType.SINGLE_CHOICE
        "Multiple Choice" -> QuestionType.MULTIPLE_CHOICE
        "True/False" -> QuestionType.TRUE_FALSE
        else -> null // Fill in the Blank, Short Answer, Unknown
    }

    private fun letterToIndex(letter: String): Int {
        if (letter.isBlank()) return 0
        val index = "ABCDEFGH".indexOf(letter.uppercase())
        return if (index >= 0) index else 0
    }

    private fun lettersToIndices(letters: String): List<Int> {
        if (letters.isBlank()) return listOf(0)
        return letters.uppercase().mapNotNull { ch ->
            val index = "ABCDEFGH".indexOf(ch)
            if (index >= 0) index else null
        }.ifEmpty { listOf(0) }
    }

    override fun decodeHtmlFromJs(raw: String?): String {
        if (raw == null) {
            Timber.w("decodeHtmlFromJs: input is null")
            return ""
        }

        if (raw.isBlank()) {
            Timber.w("decodeHtmlFromJs: input is blank")
            return ""
        }

        val trimmedInput = raw.trim()

        return try {
            val decoded = if (trimmedInput.startsWith("\"") && trimmedInput.endsWith("\"")) {
                parseJsonString(trimmedInput)
            } else {
                manualUnescape(trimmedInput)
            }

            if (decoded.isBlank()) {
                Timber.w("decodeHtmlFromJs: decoded result is blank for input length=${trimmedInput.length}")
                return ""
            }

            Timber.d("decodeHtmlFromJs: successfully decoded ${decoded.length} chars from input ${trimmedInput.length} chars")
            decoded
        } catch (e: Exception) {
            Timber.e(e, "decodeHtmlFromJs: decoding failed for input (length=${trimmedInput.length}, preview=${trimmedInput.take(100)})")
            manualUnescape(trimmedInput).take(10000)
        }
    }

    private fun parseJsonString(json: String): String {
        val sb = StringBuilder()
        var i = 1 // skip opening quote
        while (i < json.length - 1) { // skip closing quote
            when {
                json[i] == '\\' && i + 1 < json.length - 1 -> {
                    when (json[i + 1]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('')
                        'u' -> {
                            if (i + 5 < json.length) {
                                val hex = json.substring(i + 2, i + 6)
                                sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                                i += 4
                            }
                        }
                        else -> sb.append(json[i + 1])
                    }
                    i += 2
                }
                else -> {
                    sb.append(json[i])
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun manualUnescape(raw: String): String {
        return raw.replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
