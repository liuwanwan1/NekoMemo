package mirujam.nekomemo.data.repository

import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionCountByBank
import mirujam.nekomemo.data.local.entity.QuestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionBankDao: QuestionBankDao,
    private val questionDao: QuestionDao
) {

    fun getAllBanks(): Flow<List<QuestionBankEntity>> =
        questionBankDao.getAllBanks()

    suspend fun getBankById(id: Long): QuestionBankEntity? =
        questionBankDao.getBankById(id)

    suspend fun insertBank(bank: QuestionBankEntity): Long =
        questionBankDao.insertBank(bank)

    suspend fun updateBank(bank: QuestionBankEntity) =
        questionBankDao.updateBank(bank)

    suspend fun deleteBank(bank: QuestionBankEntity) =
        questionBankDao.deleteBank(bank)

    fun getBankCount(): Flow<Int> =
        questionBankDao.getBankCount()

    fun getTotalQuestionCount(): Flow<Int> =
        questionDao.getTotalQuestionCount()

    fun getQuestionCountForBank(bankId: Long): Flow<Int> =
        questionDao.getQuestionCountForBank(bankId)

    fun getQuestionCountsByBank(): Flow<List<QuestionCountByBank>> =
        questionDao.getQuestionCountsByBank()

    fun getQuestionsForBank(bankId: Long): Flow<List<QuestionEntity>> =
        questionDao.getQuestionsForBank(bankId)

    suspend fun getQuestionsForBankSync(bankId: Long): List<QuestionEntity> =
        questionDao.getQuestionsForBankSync(bankId)

    suspend fun getQuestionById(id: Long): QuestionEntity? =
        questionDao.getQuestionById(id)

    suspend fun insertQuestions(questions: List<QuestionEntity>) =
        questionDao.insertAll(questions)

    suspend fun deleteQuestion(question: QuestionEntity) =
        questionDao.deleteQuestion(question)

    suspend fun deleteQuestionById(id: Long) =
        questionDao.deleteQuestionById(id)

    suspend fun deleteAllData() {
        questionDao.deleteAll()
        questionBankDao.deleteAll()
    }
}
