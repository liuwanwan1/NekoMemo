package mirujam.nekomemo.data.mapper

import mirujam.nekomemo.data.local.ListJsonConverter
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.domain.model.QuestionType

fun QuestionBankEntity.toDomainModel(): QuestionBank = QuestionBank(
    id = id,
    title = title,
    categoryId = categoryId,
    createdAt = createdAt
)

fun QuestionBank.toEntity(): QuestionBankEntity = QuestionBankEntity(
    id = id,
    title = title,
    categoryId = categoryId,
    createdAt = createdAt
)

fun QuestionEntity.toDomainModel(): Question {
    val parsedIndices = ListJsonConverter.toIntList(correctIndices)
    val indices = parsedIndices.ifEmpty { listOf(correctIndex) }
    return Question(
        id = id,
        questionBankId = questionBankId,
        text = text,
        questionType = QuestionType.fromKey(questionType),
        options = ListJsonConverter.toStringList(options),
        correctIndex = indices.firstOrNull() ?: correctIndex,
        correctIndices = indices
    )
}

fun QuestionEntity.toDomainModel(bookmarkedIds: Set<Long>): Question {
    val base = toDomainModel()
    return base.copy(isBookmarked = base.id in bookmarkedIds)
}

fun Question.toEntity(): QuestionEntity {
    // Ensure correctIndex is always the first element of correctIndices to maintain consistency
    val consistentIndices = correctIndices.ifEmpty { listOf(correctIndex) }
    val firstIndex = consistentIndices.firstOrNull() ?: correctIndex
    return QuestionEntity(
        id = id,
        questionBankId = questionBankId,
        text = text,
        questionType = questionType.key,
        options = ListJsonConverter.fromStringList(options),
        correctIndex = firstIndex,
        correctIndices = ListJsonConverter.fromIntList(consistentIndices)
    )
}

fun List<QuestionBankEntity>.toDomainBankModels(): List<QuestionBank> = map { it.toDomainModel() }

fun List<QuestionEntity>.toDomainQuestionModels(): List<Question> = map { it.toDomainModel() }

fun List<QuestionEntity>.toDomainQuestionModels(bookmarkedIds: Set<Long>): List<Question> =
    map { it.toDomainModel(bookmarkedIds) }
