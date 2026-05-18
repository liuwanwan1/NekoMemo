package mirujam.nekomemo.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.entity.QuestionCountByBank
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.domain.model.toDomainModel
import mirujam.nekomemo.domain.model.toDomainModels
import mirujam.nekomemo.domain.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionBankDao: QuestionBankDao,
    private val questionDao: QuestionDao
) {

    private val converters = Converters()

    fun getAllBanks(): Flow<List<QuestionBank>> =
        questionBankDao.getAllBanks().map { it.toDomainModels() }

    suspend fun getBankById(id: Long): QuestionBank? =
        questionBankDao.getBankById(id)?.toDomainModel()

    suspend fun insertBank(bank: QuestionBank): Long =
        questionBankDao.insertBank(bank.toEntity())

    suspend fun updateBank(bank: QuestionBank) =
        questionBankDao.updateBank(bank.toEntity())

    suspend fun deleteBank(bank: QuestionBank) =
        questionBankDao.deleteBank(bank.toEntity())

    fun getBankCount(): Flow<Int> =
        questionBankDao.getBankCount()

    fun getTotalQuestionCount(): Flow<Int> =
        questionDao.getTotalQuestionCount()

    fun getQuestionCountsByBank(): Flow<List<QuestionCountByBank>> =
        questionDao.getQuestionCountsByBank()

    fun getQuestionCountForBank(bankId: Long): Flow<Int> =
        questionDao.getQuestionCountForBank(bankId)

    fun getQuestionsForBank(bankId: Long): Flow<List<Question>> =
        questionDao.getQuestionsForBank(bankId).map { it.toDomainModels(converters) }

    fun getPagedQuestionsForBank(bankId: Long): Flow<PagingData<Question>> =
        Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { questionDao.getPagedQuestionsForBank(bankId) }
        ).flow.map { pagingData -> pagingData.map { it.toDomainModel(converters) } }

    suspend fun getQuestionsForBankSync(bankId: Long): List<Question> =
        questionDao.getQuestionsForBankSync(bankId).toDomainModels(converters)

    suspend fun getQuestionById(id: Long): Question? =
        questionDao.getQuestionById(id)?.toDomainModel(converters)

    suspend fun insertQuestions(questions: List<Question>) {
        if (questions.isNotEmpty()) {
            questionDao.insertAll(questions.map { it.toEntity(converters) })
        }
    }

    suspend fun updateQuestionWithVersionCheck(
        id: Long,
        text: String,
        options: List<String>,
        correctIndex: Int,
        expectedVersion: Int
    ): Boolean {
        val updatedRows = questionDao.updateWithVersionCheck(id, text, converters.fromStringList(options), correctIndex, expectedVersion)
        return updatedRows > 0
    }

    suspend fun deleteQuestion(question: Question) =
        questionDao.deleteQuestion(question.toEntity(converters))

    suspend fun deleteAllData() {
        questionDao.deleteAll()
        questionBankDao.deleteAll()
    }

    suspend fun duplicateBank(bankId: Long): Long {
        val originalBank = questionBankDao.getBankById(bankId) ?: return -1L
        val newBankId = questionBankDao.insertBank(
            originalBank.copy(
                id = 0,
                title = "${originalBank.title} (Copy)",
                createdAt = System.currentTimeMillis()
            )
        )
        val questions = questionDao.getQuestionsForBankSync(bankId)
        if (questions.isNotEmpty()) {
            questionDao.insertAll(questions.map { it.copy(id = 0, questionBankId = newBankId) })
        }
        return newBankId
    }
}
