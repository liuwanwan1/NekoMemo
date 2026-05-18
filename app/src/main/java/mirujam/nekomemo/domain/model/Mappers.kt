package mirujam.nekomemo.domain.model

import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity

fun QuestionBankEntity.toDomainModel(): QuestionBank = QuestionBank(
    id = id,
    title = title,
    category = category,
    createdAt = createdAt
)

fun QuestionBank.toEntity(): QuestionBankEntity = QuestionBankEntity(
    id = id,
    title = title,
    category = category,
    createdAt = createdAt
)

fun QuestionEntity.toDomainModel(converters: Converters): Question = Question(
    id = id,
    questionBankId = questionBankId,
    text = text,
    options = converters.toStringList(options),
    correctIndex = correctIndex,
    version = version
)

fun Question.toEntity(converters: Converters): QuestionEntity = QuestionEntity(
    id = id,
    questionBankId = questionBankId,
    text = text,
    options = converters.fromStringList(options),
    correctIndex = correctIndex,
    version = version
)

fun List<QuestionBankEntity>.toDomainModels(): List<QuestionBank> = map { it.toDomainModel() }

fun List<QuestionEntity>.toDomainModels(converters: Converters): List<Question> = map { it.toDomainModel(converters) }
