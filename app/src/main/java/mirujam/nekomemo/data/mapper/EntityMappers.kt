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

fun QuestionEntity.toDomainModel(): Question = Question(
    id = id,
    questionBankId = questionBankId,
    text = text,
    questionType = QuestionType.fromKey(questionType),
    options = ListJsonConverter.toStringList(options),
    correctIndex = correctIndex,
    correctIndices = ListJsonConverter.toIntList(correctIndices).ifEmpty { listOf(correctIndex) }
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    questionBankId = questionBankId,
    text = text,
    questionType = questionType.key,
    options = ListJsonConverter.fromStringList(options),
    correctIndex = correctIndex,
    correctIndices = ListJsonConverter.fromIntList(correctIndices)
)

fun List<QuestionBankEntity>.toDomainBankModels(): List<QuestionBank> = map { it.toDomainModel() }

fun List<QuestionEntity>.toDomainQuestionModels(): List<Question> = map { it.toDomainModel() }
