package mirujam.nekomemo.domain.usecase

import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankExportImportUseCase @Inject constructor(
    private val questionBankDao: QuestionBankDao,
    private val questionDao: QuestionDao,
    private val converters: Converters
) {

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
