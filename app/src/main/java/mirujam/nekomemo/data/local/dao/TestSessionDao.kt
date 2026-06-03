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

    @Query("""
        SELECT s.id, s.bankId, b.title as bankTitle,
               s.totalQuestions, s.correctCount, s.wrongCount, s.unansweredCount,
               s.percentage, s.durationMs, s.createdAt
        FROM test_sessions s
        INNER JOIN question_banks b ON s.bankId = b.id
        ORDER BY s.createdAt DESC
    """)
    fun getAllSessionsWithBankTitle(): Flow<List<TestSessionWithBankTitle>>

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

data class TestSessionWithBankTitle(
    val id: Long,
    val bankId: Long,
    val bankTitle: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val percentage: Int,
    val durationMs: Long,
    val createdAt: Long
)
