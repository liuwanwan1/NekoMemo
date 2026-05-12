package mirujam.nekomemo.domain.mapper

import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.ui.model.QuestionUiModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionMapper @Inject constructor(
    private val converters: Converters
) {

    fun mapToUiModel(entity: QuestionEntity): QuestionUiModel {
        return QuestionUiModel(
            id = entity.id,
            text = entity.text,
            options = converters.toStringList(entity.options),
            correctIndex = entity.correctIndex
        )
    }

    fun mapToUiModels(entities: List<QuestionEntity>): List<QuestionUiModel> {
        return entities.map { mapToUiModel(it) }
    }

    fun mapOptionsToJson(options: List<String>): String {
        return converters.fromStringList(options)
    }

    fun mapJsonToOptions(optionsJson: String): List<String> {
        return converters.toStringList(optionsJson)
    }
}
