package mirujam.nekomemo.data.mapper

import mirujam.nekomemo.data.local.ListJsonConverter
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank

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

fun QuestionEntity.toDomainModel(): Question = Question(
    id = id,
    questionBankId = questionBankId,
    text = text,
    options = ListJsonConverter.toStringList(options),
    correctIndex = correctIndex,
    version = version
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    questionBankId = questionBankId,
    text = text,
    options = ListJsonConverter.fromStringList(options),
    correctIndex = correctIndex,
    version = version
)

fun List<QuestionBankEntity>.toDomainBankModels(): List<QuestionBank> = map { it.toDomainModel() }

fun List<QuestionEntity>.toDomainQuestionModels(): List<Question> = map { it.toDomainModel() }
