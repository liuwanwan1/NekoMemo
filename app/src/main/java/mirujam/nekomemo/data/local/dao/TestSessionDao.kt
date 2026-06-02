package mirujam.nekomemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.entity.TestSessionEntity

@Dao
interface TestSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TestSessionEntity): Long

    @Query("SELECT * FROM test_sessions WHERE bankId = :bankId ORDER BY createdAt DESC")
    fun getSessionsForBank(bankId: Long): Flow<List<TestSessionEntity>>

    @Query("SELECT * FROM test_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<TestSessionEntity>>

    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TestSessionEntity?

    @Query("DELETE FROM test_sessions WHERE bankId = :bankId")
    suspend fun deleteSessionsForBank(bankId: Long)

    @Query("DELETE FROM test_sessions")
    suspend fun deleteAll()

    @Query("""
        SELECT COUNT(*) as totalSessions,
               SUM(correctCount) as totalCorrect,
               SUM(totalQuestions) as totalQuestions,
               AVG(percentage) as avgPercentage
        FROM test_sessions WHERE bankId = :bankId
    """)
    fun getBankStatistics(bankId: Long): Flow<BankStatistics?>

    @Query("""
        SELECT COUNT(*) as totalSessions,
               SUM(correctCount) as totalCorrect,
               SUM(totalQuestions) as totalQuestions,
               AVG(percentage) as avgPercentage
        FROM test_sessions
    """)
    fun getOverallStatistics(): Flow<BankStatistics?>
}

data class BankStatistics(
    val totalSessions: Int,
    val totalCorrect: Int?,
    val totalQuestions: Int?,
    val avgPercentage: Double?
)
