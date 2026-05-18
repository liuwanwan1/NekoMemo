package mirujam.nekomemo.ui.model

import androidx.compose.runtime.Immutable
import mirujam.nekomemo.data.local.Converters

data class QuestionUiModel(
    val id: Long,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    companion object {
        fun fromEntity(entity: mirujam.nekomemo.data.local.entity.QuestionEntity, converters: Converters): QuestionUiModel {
            return QuestionUiModel(
                id = entity.id,
                text = entity.text,
                options = converters.toStringList(entity.options),
                correctIndex = entity.correctIndex
            )
        }

        fun fromEntities(entities: List<mirujam.nekomemo.data.local.entity.QuestionEntity>, converters: Converters): List<QuestionUiModel> {
            return entities.map { fromEntity(it, converters) }
        }
    }
}

@Immutable
data class CachedQuestion(
    val id: Long,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    companion object {
        fun fromEntity(entity: mirujam.nekomemo.data.local.entity.QuestionEntity, 
                      optionList: List<String>): CachedQuestion {
            return CachedQuestion(
                id = entity.id,
                text = entity.text,
                options = optionList,
                correctIndex = entity.correctIndex
            )
        }
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
                if (selected == null) {
                    unanswered++
                } else if (selected == question.correctIndex) {
                    correct++
                } else {
                    wrong++
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
