package mirujam.nekomemo.ui.model

import androidx.compose.runtime.Immutable
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionType

@Immutable
data class QuestionUiModel(
    val id: Long,
    val text: String,
    val questionType: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String>,
    val correctIndex: Int,
    val correctIndices: List<Int> = listOf(correctIndex)
) {
    val isMultipleChoice: Boolean get() = questionType == QuestionType.MULTIPLE_CHOICE
    val isTrueFalse: Boolean get() = questionType == QuestionType.TRUE_FALSE

    companion object {
        fun fromDomainModel(question: Question): QuestionUiModel = QuestionUiModel(
            id = question.id,
            text = question.text,
            questionType = question.questionType,
            options = question.options,
            correctIndex = question.correctIndex,
            correctIndices = question.correctIndices
        )

        fun fromDomainModels(questions: List<Question>): List<QuestionUiModel> =
            questions.map { fromDomainModel(it) }
    }
}

data class ScoreModel(
    val correct: Int,
    val wrong: Int,
    val unanswered: Int,
    val total: Int,
    val percentage: Int
) {
    companion object {
        fun calculate(
            questions: List<QuestionUiModel>,
            selectedAnswers: Map<Int, Set<Int>>
        ): ScoreModel {
            var correct = 0
            var wrong = 0
            var unanswered = 0
            questions.forEachIndexed { index, question ->
                val selected = selectedAnswers[index]
                when {
                    selected == null || selected.isEmpty() -> unanswered++
                    question.isMultipleChoice -> {
                        // Multi-choice: must select ALL correct answers and NO wrong ones
                        if (selected == question.correctIndices.toSet()) correct++ else wrong++
                    }
                    else -> {
                        // Single choice / True-False: first selected must match correctIndex
                        if (selected.size == 1 && selected.first() == question.correctIndex) correct++ else wrong++
                    }
                }
            }
            val total = questions.size
            val percentage = if (total > 0) (correct * 100) / total else 0
            return ScoreModel(correct, wrong, unanswered, total, percentage)
        }
    }
}
