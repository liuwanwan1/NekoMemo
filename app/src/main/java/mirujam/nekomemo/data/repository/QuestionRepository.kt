package mirujam.nekomemo.data.repository

import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionBankDao: QuestionBankDao,
    private val questionDao: QuestionDao,
    private val converters: Converters
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

    fun getQuestionCountForBank(bankId: Long): Flow<Int> =
        questionBankDao.getQuestionCountForBank(bankId)

    fun getTotalQuestionCount(): Flow<Int> =
        questionDao.getTotalQuestionCount()

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

    suspend fun exportBankToJson(bankId: Long): String? {
        val bank = questionBankDao.getBankById(bankId) ?: return null
        val questions = questionDao.getQuestionsForBankSync(bankId)

        val json = JSONObject()
        json.put("title", bank.title)
        json.put("category", bank.category)

        val questionsArray = JSONArray()
        questions.forEach { q ->
            val qJson = JSONObject()
            qJson.put("text", q.text)
            qJson.put("options", JSONArray(converters.toStringList(q.options)))
            qJson.put("correctIndex", q.correctIndex)
            questionsArray.put(qJson)
        }
        json.put("questions", questionsArray)

        val wrapper = JSONObject()
        wrapper.put("nekomemo", json)
        return wrapper.toString(2)
    }

    suspend fun importBankFromJson(jsonString: String): Long {
        val wrapper = JSONObject(jsonString)
        val bankJson = wrapper.optJSONObject("nekomemo") ?: wrapper

        val title = bankJson.optString("title", "Imported Bank")
        val category = bankJson.optString("category", "General")

        val bankId = questionBankDao.insertBank(
            QuestionBankEntity(title = title, category = category)
        )

        val questionsArray = bankJson.optJSONArray("questions") ?: return bankId
        val entities = (0 until questionsArray.length()).map { i ->
            val qJson = questionsArray.getJSONObject(i)
            val optionsArray = qJson.optJSONArray("options")
            val options = if (optionsArray != null) {
                converters.fromStringList(
                    (0 until optionsArray.length()).map { j -> optionsArray.getString(j) })
            } else {
                converters.fromStringList(emptyList())
            }

            QuestionEntity(
                questionBankId = bankId,
                text = qJson.optString("text", ""),
                options = options,
                correctIndex = qJson.optInt("correctIndex", 0)
            )
        }

        if (entities.isNotEmpty()) {
            questionDao.insertAll(entities)
        }

        return bankId
    }
}
