package mirujam.nekomemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.entity.WrongQuestionEntity

@Dao
interface WrongQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wrongQuestion: WrongQuestionEntity): Long

    @Update
    suspend fun update(wrongQuestion: WrongQuestionEntity)

    @Query("SELECT * FROM wrong_questions WHERE bankId = :bankId AND isResolved = 0 ORDER BY lastWrongAt DESC")
    fun getUnresolvedForBank(bankId: Long): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE isResolved = 0 ORDER BY lastWrongAt DESC")
    fun getAllUnresolved(): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE questionId = :questionId AND isResolved = 0 LIMIT 1")
    suspend fun getUnresolvedByQuestionId(questionId: Long): WrongQuestionEntity?

    @Query("SELECT * FROM wrong_questions WHERE id = :id")
    suspend fun getById(id: Long): WrongQuestionEntity?

    @Query("UPDATE wrong_questions SET isResolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)

    @Query("UPDATE wrong_questions SET isResolved = 1 WHERE questionId = :questionId AND isResolved = 0")
    suspend fun markResolvedByQuestionId(questionId: Long)

    @Query("DELETE FROM wrong_questions WHERE bankId = :bankId")
    suspend fun deleteForBank(bankId: Long)

    @Query("DELETE FROM wrong_questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM wrong_questions WHERE bankId = :bankId AND isResolved = 0")
    fun getUnresolvedCountForBank(bankId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM wrong_questions WHERE isResolved = 0")
    fun getTotalUnresolvedCount(): Flow<Int>

    @Query("SELECT DISTINCT bankId FROM wrong_questions WHERE isResolved = 0")
    fun getUnresolvedBankIds(): Flow<List<Long>>

    @Query("SELECT questionId FROM wrong_questions WHERE isResolved = 0 AND bankId = :bankId")
    suspend fun getUnresolvedQuestionIdsForBank(bankId: Long): List<Long>

    @Query("SELECT questionId FROM wrong_questions WHERE isResolved = 0")
    suspend fun getAllUnresolvedQuestionIds(): List<Long>
}
