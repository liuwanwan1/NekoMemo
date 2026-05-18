package mirujam.nekomemo.ui.model

import androidx.compose.runtime.Immutable
import mirujam.nekomemo.domain.model.Question

@Immutable
data class QuestionUiModel(
    val id: Long,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    companion object {
        fun fromDomainModel(question: Question): QuestionUiModel = QuestionUiModel(
            id = question.id,
            text = question.text,
            options = question.options,
            correctIndex = question.correctIndex
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
            selectedAnswers: Map<Int, Int>
        ): ScoreModel {
            var correct = 0
            var wrong = 0
            var unanswered = 0
            questions.forEachIndexed { index, question ->
                val selected = selectedAnswers[index]
                when (selected) {
                    null -> unanswered++
                    question.correctIndex -> correct++
                    else -> wrong++
                }
            }
            val total = questions.size
            val percentage = if (total > 0) (correct * 100) / total else 0
            return ScoreModel(correct, wrong, unanswered, total, percentage)
        }
    }
}

data class FetcherUiState(
    val isParsing: Boolean = false,
    val parseResult: UiText? = null,
    val currentUrl: String = "https://i.chaoxing.com",
    val urlInput: String = "https://i.chaoxing.com",
    val navigateToExtract: Boolean = false,
    val extractedJson: String? = null
)
