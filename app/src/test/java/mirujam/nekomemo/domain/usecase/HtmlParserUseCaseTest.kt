package mirujam.nekomemo.domain.usecase

import mirujam.nekomemo.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlParserUseCaseTest {

    private val parser = HtmlParserUseCase()

    @Test
    fun parse_singleChoice_extractsCorrectly() {
        val html = """
            <html>
            <body>
                <h2 class="mark_title">Math Quiz</h2>
                <div class="questionLi">
                    <h3 class="mark_name"><span class="colorShallow">单选题</span> 1. What is 2+2?</h3>
                    <ul class="mark_letter">
                        <li>A. 3</li>
                        <li>B. 4</li>
                        <li>C. 5</li>
                    </ul>
                    <span class="rightAnswerContent">B</span>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals("Math Quiz", result.name)
        assertEquals(1, result.questions.size)
        assertEquals("What is 2+2?", result.questions[0].content)
        assertEquals(QuestionType.SINGLE_CHOICE, result.questions[0].questionType)
        assertEquals(listOf("3", "4", "5"), result.questions[0].options)
        assertEquals("B", result.questions[0].correctAnswer)
        assertEquals(listOf(1), result.questions[0].correctIndices)
    }

    @Test
    fun parse_multipleChoice_extractsCorrectly() {
        val html = """
            <html>
            <body>
                <h2 class="mark_title">Science Quiz</h2>
                <div class="questionLi">
                    <h3 class="mark_name"><span class="colorShallow">多选题</span> 1. Select primes</h3>
                    <ul class="mark_letter">
                        <li>A. 2</li>
                        <li>B. 3</li>
                        <li>C. 4</li>
                        <li>D. 5</li>
                    </ul>
                    <span class="rightAnswerContent">ABD</span>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals(1, result.questions.size)
        assertEquals(QuestionType.MULTIPLE_CHOICE, result.questions[0].questionType)
        assertEquals(listOf("2", "3", "4", "5"), result.questions[0].options)
        assertEquals("ABD", result.questions[0].correctAnswer)
        assertEquals(listOf(0, 1, 3), result.questions[0].correctIndices)
    }

    @Test
    fun parse_trueFalse_extractsCorrectly() {
        val html = """
            <html>
            <body>
                <h2 class="mark_title">Logic Quiz</h2>
                <div class="questionLi">
                    <h3 class="mark_name"><span class="colorShallow">判断题</span> 1. Earth is flat</h3>
                    <span class="rightAnswerContent">B</span>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals(1, result.questions.size)
        assertEquals(QuestionType.TRUE_FALSE, result.questions[0].questionType)
        assertEquals(listOf("对", "错"), result.questions[0].options)
        assertEquals("B", result.questions[0].correctAnswer)
    }

    @Test
    fun parse_emptyHtml_returnsEmptyBank() {
        val result = parser.parse("")

        assertEquals("", result.name)
        assertTrue(result.questions.isEmpty())
    }

    @Test
    fun parse_noQuestions_returnsEmptyList() {
        val html = "<html><body><h2 class=\"mark_title\">Empty</h2></body></html>"

        val result = parser.parse(html)

        assertEquals("Empty", result.name)
        assertTrue(result.questions.isEmpty())
    }

    @Test
    fun decodeHtmlFromJs_decodesJsonWrappedString() {
        val encoded = "\"Hello \\\"World\\\"\""

        val result = parser.decodeHtmlFromJs(encoded)

        assertEquals("Hello \"World\"", result)
    }

    @Test
    fun decodeHtmlFromJs_nullReturnsEmpty() {
        assertEquals("", parser.decodeHtmlFromJs(null))
    }

    @Test
    fun decodeHtmlFromJs_blankReturnsEmpty() {
        assertEquals("", parser.decodeHtmlFromJs("   "))
    }

    @Test
    fun decodeHtmlFromJs_fallbackForInvalidJson() {
        val input = "Hello \\\"World\\\""

        val result = parser.decodeHtmlFromJs(input)

        assertEquals("Hello \"World\"", result)
    }

    @Test
    fun parse_skipsUnsupportedTypes() {
        val html = """
            <html>
            <body>
                <h2 class="mark_title">Mixed Quiz</h2>
                <div class="questionLi">
                    <h3 class="mark_name"><span class="colorShallow">填空题</span> 1. Fill in ___</h3>
                </div>
                <div class="questionLi">
                    <h3 class="mark_name"><span class="colorShallow">简答题</span> 2. Explain</h3>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html)

        assertTrue(result.questions.isEmpty())
        assertEquals(2, result.unsupportedTypeCount)
    }
}
