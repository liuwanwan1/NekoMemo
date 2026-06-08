package mirujam.nekomemo.ui.model

import mirujam.nekomemo.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreModelTest {

    @Test
    fun calculate_countsCorrectWrongAndUnanswered() {
        val questions = listOf(
            QuestionUiModel(id = 1, text = "q1", options = listOf("A", "B"), correctIndex = 0),
            QuestionUiModel(id = 2, text = "q2", options = listOf("A", "B"), correctIndex = 1),
            QuestionUiModel(id = 3, text = "q3", options = listOf("A", "B"), correctIndex = 1)
        )

        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0), 1 to setOf(0))
        )

        assertEquals(1, score.correct)
        assertEquals(1, score.wrong)
        assertEquals(1, score.unanswered)
        assertEquals(3, score.total)
        assertEquals(33, score.percentage)
    }

    @Test
    fun calculate_handlesEmptyQuestions() {
        val score = ScoreModel.calculate(emptyList(), emptyMap())

        assertEquals(0, score.correct)
        assertEquals(0, score.wrong)
        assertEquals(0, score.unanswered)
        assertEquals(0, score.total)
        assertEquals(0, score.percentage)
    }

    @Test
    fun calculate_multipleChoiceAllCorrect() {
        val questions = listOf(
            QuestionUiModel(
                id = 1,
                text = "q1",
                questionType = QuestionType.MULTIPLE_CHOICE,
                options = listOf("A", "B", "C"),
                correctIndex = 0,
                correctIndices = listOf(0, 2)
            )
        )

        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0, 2))
        )

        assertEquals(1, score.correct)
        assertEquals(0, score.wrong)
        assertEquals(0, score.unanswered)
        assertEquals(100, score.percentage)
    }

    @Test
    fun calculate_multipleChoicePartialSelectionIsWrong() {
        val questions = listOf(
            QuestionUiModel(
                id = 1,
                text = "q1",
                questionType = QuestionType.MULTIPLE_CHOICE,
                options = listOf("A", "B", "C"),
                correctIndex = 0,
                correctIndices = listOf(0, 2)
            )
        )

        // Only selected one correct answer, missing the other
        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0))
        )

        assertEquals(0, score.correct)
        assertEquals(1, score.wrong)
        assertEquals(0, score.unanswered)
    }

    @Test
    fun calculate_multipleChoiceWithExtraWrongSelectionIsWrong() {
        val questions = listOf(
            QuestionUiModel(
                id = 1,
                text = "q1",
                questionType = QuestionType.MULTIPLE_CHOICE,
                options = listOf("A", "B", "C"),
                correctIndex = 0,
                correctIndices = listOf(0, 2)
            )
        )

        // Selected all correct plus one wrong
        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0, 1, 2))
        )

        assertEquals(0, score.correct)
        assertEquals(1, score.wrong)
    }

    @Test
    fun calculate_trueFalseCorrect() {
        val questions = listOf(
            QuestionUiModel(
                id = 1,
                text = "q1",
                questionType = QuestionType.TRUE_FALSE,
                options = listOf("True", "False"),
                correctIndex = 0
            )
        )

        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0))
        )

        assertEquals(1, score.correct)
        assertEquals(0, score.wrong)
        assertEquals(100, score.percentage)
    }

    @Test
    fun calculate_singleChoiceWithMultipleSelectionsIsWrong() {
        val questions = listOf(
            QuestionUiModel(id = 1, text = "q1", options = listOf("A", "B"), correctIndex = 0)
        )

        // Selected two options for a single-choice question
        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0, 1))
        )

        assertEquals(0, score.correct)
        assertEquals(1, score.wrong)
    }

    @Test
    fun calculate_percentageRoundsDown() {
        val questions = listOf(
            QuestionUiModel(id = 1, text = "q1", options = listOf("A", "B"), correctIndex = 0),
            QuestionUiModel(id = 2, text = "q2", options = listOf("A", "B"), correctIndex = 0),
            QuestionUiModel(id = 3, text = "q3", options = listOf("A", "B"), correctIndex = 0)
        )

        // 1 out of 3 = 33.33% -> rounds down to 33
        val score = ScoreModel.calculate(
            questions = questions,
            selectedAnswers = mapOf(0 to setOf(0))
        )

        assertEquals(33, score.percentage)
    }
}
