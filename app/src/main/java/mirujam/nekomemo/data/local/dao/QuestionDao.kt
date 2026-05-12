package mirujam.nekomemo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.entity.QuestionCountByBank
import mirujam.nekomemo.data.local.entity.QuestionEntity

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE questionBankId = :bankId ORDER BY id")
    fun getQuestionsForBank(bankId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE questionBankId = :bankId ORDER BY id")
    suspend fun getQuestionsForBankSync(bankId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    @Query("DELETE FROM questions WHERE questionBankId = :bankId")
    suspend fun deleteQuestionsForBank(bankId: Long)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM questions")
    fun getTotalQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE questionBankId = :bankId")
    fun getQuestionCountForBank(bankId: Long): Flow<Int>

    @Query("SELECT questionBankId, COUNT(*) as count FROM questions GROUP BY questionBankId")
    fun getQuestionCountsByBank(): Flow<List<QuestionCountByBank>>
}
