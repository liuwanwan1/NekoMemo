package mirujam.nekomemo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mirujam.nekomemo.data.local.dao.BankStatistics
import mirujam.nekomemo.data.local.dao.TestSessionDao
import mirujam.nekomemo.data.local.dao.TestSessionWithBankTitle
import mirujam.nekomemo.data.local.dao.WrongQuestionDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.local.entity.TestSessionEntity
import mirujam.nekomemo.data.local.entity.WrongQuestionEntity
import mirujam.nekomemo.data.mapper.toDomainModel
import mirujam.nekomemo.domain.model.Question
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestHistoryRepository @Inject constructor(
    private val testSessionDao: TestSessionDao,
    private val wrongQuestionDao: WrongQuestionDao,
    private val questionDao: QuestionDao
) {

    // Test Session operations
    suspend fun recordTestSession(
        bankId: Long,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        unansweredCount: Int,
        percentage: Int,
        durationMs: Long
    ): Long {
        return testSessionDao.insert(
            TestSessionEntity(
                bankId = bankId,
                totalQuestions = totalQuestions,
                correctCount = correctCount,
                wrongCount = wrongCount,
                unansweredCount = unansweredCount,
                percentage = percentage,
                durationMs = durationMs
            )
        )
    }

    fun getSessionsForBank(bankId: Long): Flow<List<TestSessionEntity>> =
        testSessionDao.getSessionsForBank(bankId)

    fun getAllSessions(): Flow<List<TestSessionEntity>> =
        testSessionDao.getAllSessions()

    fun getAllSessionsWithBankTitle(): Flow<List<TestSessionWithBankTitle>> =
        testSessionDao.getAllSessionsWithBankTitle()

    fun getBankStatistics(bankId: Long): Flow<BankStatistics?> =
        testSessionDao.getBankStatistics(bankId)

    fun getOverallStatistics(): Flow<BankStatistics?> =
        testSessionDao.getOverallStatistics()

    // Wrong Question operations
    suspend fun recordWrongQuestion(questionId: Long, bankId: Long) {
        val existing = wrongQuestionDao.getUnresolvedByQuestionId(questionId)
        if (existing != null) {
            wrongQuestionDao.update(
                existing.copy(
                    wrongCount = existing.wrongCount + 1,
                    lastWrongAt = System.currentTimeMillis()
                )
            )
        } else {
            wrongQuestionDao.insert(
                WrongQuestionEntity(
                    questionId = questionId,
                    bankId = bankId
                )
            )
        }
    }

    suspend fun markWrongQuestionResolved(wrongQuestionId: Long) {
        wrongQuestionDao.markResolved(wrongQuestionId)
    }

    suspend fun markQuestionResolved(questionId: Long) {
        wrongQuestionDao.markResolvedByQuestionId(questionId)
    }

    fun getUnresolvedWrongQuestions(bankId: Long): Flow<List<WrongQuestionWithQuestion>> =
        wrongQuestionDao.getUnresolvedWithQuestionsForBank(bankId).map { items ->
            items.map { item ->
                WrongQuestionWithQuestion(
                    wrongQuestion = WrongQuestionEntity(
                        id = item.id,
                        questionId = item.questionId,
                        bankId = item.bankId,
                        wrongCount = item.wrongCount,
                        lastWrongAt = item.lastWrongAt,
                        isResolved = item.isResolved
                    ),
                    question = QuestionEntity(
                        id = item.q_id,
                        questionBankId = item.q_questionBankId,
                        text = item.q_text,
                        questionType = item.q_questionType,
                        options = item.q_options,
                        correctIndex = item.q_correctIndex,
                        correctIndices = item.q_correctIndices
                    ).toDomainModel()
                )
            }
        }

    fun getAllUnresolvedWrongQuestions(): Flow<List<WrongQuestionWithQuestion>> =
        wrongQuestionDao.getAllUnresolvedWithQuestions().map { items ->
            items.map { item ->
                WrongQuestionWithQuestion(
                    wrongQuestion = WrongQuestionEntity(
                        id = item.id,
                        questionId = item.questionId,
                        bankId = item.bankId,
                        wrongCount = item.wrongCount,
                        lastWrongAt = item.lastWrongAt,
                        isResolved = item.isResolved
                    ),
                    question = QuestionEntity(
                        id = item.q_id,
                        questionBankId = item.q_questionBankId,
                        text = item.q_text,
                        questionType = item.q_questionType,
                        options = item.q_options,
                        correctIndex = item.q_correctIndex,
                        correctIndices = item.q_correctIndices
                    ).toDomainModel()
                )
            }
        }

    fun getUnresolvedCountForBank(bankId: Long): Flow<Int> =
        wrongQuestionDao.getUnresolvedCountForBank(bankId)

    fun getTotalUnresolvedCount(): Flow<Int> =
        wrongQuestionDao.getTotalUnresolvedCount()

    fun getUnresolvedBankIds(): Flow<List<Long>> =
        wrongQuestionDao.getUnresolvedBankIds()

    suspend fun getUnresolvedQuestionIdsForBank(bankId: Long): List<Long> =
        wrongQuestionDao.getUnresolvedQuestionIdsForBank(bankId)

    suspend fun getAllUnresolvedQuestionIds(): List<Long> =
        wrongQuestionDao.getAllUnresolvedQuestionIds()

    suspend fun deleteAllData() {
        testSessionDao.deleteAll()
        wrongQuestionDao.deleteAll()
    }
}

data class WrongQuestionWithQuestion(
    val wrongQuestion: WrongQuestionEntity,
    val question: Question
)
